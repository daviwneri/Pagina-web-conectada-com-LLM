package com.devweb.gateway.sse;

import com.devweb.gateway.chat.RespostaMessage;
import com.devweb.gateway.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RespostaListener {

    private final SseRegistry registry;

    public RespostaListener(SseRegistry registry) {
        this.registry = registry;
    }

    @RabbitListener(queues = RabbitConfig.RESPOSTAS_QUEUE)
    public void receber(RespostaMessage resposta) {
        registry.entregar(resposta);
    }
}
