package com.devweb.worker.config;

import com.devweb.worker.listener.PerguntaListener;
import com.devweb.worker.model.PerguntaMessage;
import com.devweb.worker.model.RespostaMessage;
import com.devweb.worker.service.LlmService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoteamentoTest {
    @Test
    void publicaSucessoEFalhaComChavesDistintasNaTopologiaDeRespostas() {
        RabbitConfig config = new RabbitConfig();
        var binding = config.respostasBinding();
        assertEquals("topic", config.respostasExchange().getType());
        assertEquals(config.respostasExchange().getName(), binding.getExchange());
        assertEquals("respostas.queue", binding.getDestination());
        assertEquals("resposta.*", binding.getRoutingKey());
        LlmService llm = mock(LlmService.class);
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        PerguntaListener listener = new PerguntaListener(llm, rabbit);
        when(llm.responder("p1")).thenReturn("r1");
        when(llm.responder("p2")).thenThrow(new IllegalStateException("falha simulada"));
        listener.processar(new PerguntaMessage("a", "ana", "p1"));
        listener.processar(new PerguntaMessage("b", "ana", "p2"));
        verify(rabbit).convertAndSend(binding.getExchange(), "resposta.a",
                new RespostaMessage("a", "ana", "r1", false));
        var falha = ArgumentCaptor.forClass(RespostaMessage.class);
        verify(rabbit).convertAndSend(eq(binding.getExchange()), eq("resposta.b"), falha.capture());
        assertTrue(falha.getValue().erro());
        assertEquals("b", falha.getValue().correlationId());
        assertEquals("ana", falha.getValue().userId());
    }
}
