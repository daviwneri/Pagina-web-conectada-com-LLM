package com.devweb.gateway.chat;

import com.devweb.gateway.config.RabbitConfig;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/perguntas")
public class PerguntaController {

    private final RabbitTemplate rabbit;
    private final RateLimiter rateLimiter;

    public PerguntaController(RabbitTemplate rabbit, RateLimiter rateLimiter) {
        this.rabbit = rabbit;
        this.rateLimiter = rateLimiter;
    }

    /** Só publica na fila e responde 202. O Gateway NUNCA chama a LLM. */
    @PostMapping
    public ResponseEntity<Map<String, String>> enviar(@Valid @RequestBody PerguntaRequest req,
                                                      Authentication auth) {
        String userId = auth.getName();
        if (!rateLimiter.permitir(userId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("erro", "Muitas perguntas seguidas. Aguarde um instante."));
        }
        String correlationId = UUID.randomUUID().toString();
        rabbit.convertAndSend(RabbitConfig.PERGUNTAS_EXCHANGE, RabbitConfig.PERGUNTAS_KEY,
                new PerguntaMessage(correlationId, userId, req.texto()));
        return ResponseEntity.accepted().body(Map.of("correlationId", correlationId));
    }
}
