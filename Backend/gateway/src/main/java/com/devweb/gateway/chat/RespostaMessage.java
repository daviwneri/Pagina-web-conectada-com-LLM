package com.devweb.gateway.chat;

/** Mensagem consumida de respostas.queue. */
public record RespostaMessage(String correlationId, String userId, String texto, boolean erro) {
}
