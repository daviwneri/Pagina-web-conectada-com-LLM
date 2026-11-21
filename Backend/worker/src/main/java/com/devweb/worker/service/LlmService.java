package com.devweb.worker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    // Máscara de sistema (prompt engineering). {pergunta} é preenchido como parâmetro.
    private static final String TEMPLATE =
            "Levando em conta que você é um tutor acadêmico de Ciência da Computação, "
            + "responda de forma didática à seguinte pergunta: {pergunta}";

    private final ChatClient chatClient;
    private final int maxTentativas;
    private final long esperaMs;

    public LlmService(ChatClient.Builder builder,
                      @Value("${app.llm.max-tentativas}") int maxTentativas,
                      @Value("${app.llm.espera-ms}") long esperaMs) {
        this.chatClient = builder.build();
        this.maxTentativas = maxTentativas;
        this.esperaMs = esperaMs;
    }

    /** Chamada BLOQUEANTE à LLM, com Retry em caso de falha/rate limit. */
    public String responder(String pergunta) {
        RuntimeException ultimoErro = null;
        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                return chatClient.prompt()
                        .user(u -> u.text(TEMPLATE).param("pergunta", pergunta))
                        .call()
                        .content();
            } catch (RuntimeException e) {
                ultimoErro = e;
                log.warn("Falha na LLM (tentativa {}/{}): {}", tentativa, maxTentativas, e.getMessage());
                if (tentativa < maxTentativas) {
                    dormir();
                }
            }
        }
        throw ultimoErro;
    }

    private void dormir() {
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
