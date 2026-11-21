package com.devweb.worker.listener;

import com.devweb.worker.config.RabbitConfig;
import com.devweb.worker.model.PerguntaMessage;
import com.devweb.worker.model.RespostaMessage;
import com.devweb.worker.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PerguntaListener {

    private static final Logger log = LoggerFactory.getLogger(PerguntaListener.class);

    private final LlmService llm;
    private final RabbitTemplate rabbit;

    public PerguntaListener(LlmService llm, RabbitTemplate rabbit) {
        this.llm = llm;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = RabbitConfig.PERGUNTAS_QUEUE)
    public void processar(PerguntaMessage pergunta) {
        log.info("Pergunta recebida [{}] de {}", pergunta.correlationId(), pergunta.userId());

        RespostaMessage resposta;
        try {
            String texto = llm.responder(pergunta.texto());
            resposta = new RespostaMessage(pergunta.correlationId(), pergunta.userId(), texto, false);
        } catch (Exception e) {
            // Esgotou as tentativas: avisa o usuário em vez de deixá-lo esperando para sempre.
            log.error("LLM indisponível para [{}]", pergunta.correlationId(), e);
            resposta = new RespostaMessage(pergunta.correlationId(), pergunta.userId(),
                    "Não consegui gerar a resposta agora. Tente novamente em instantes.", true);
        }

        rabbit.convertAndSend(RabbitConfig.RESPOSTAS_EXCHANGE, RabbitConfig.RESPOSTAS_KEY, resposta);
    }
}
