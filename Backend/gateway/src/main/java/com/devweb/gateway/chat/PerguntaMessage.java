package com.devweb.gateway.chat;

/** Mensagem publicada em perguntas.queue. */
public record PerguntaMessage(String correlationId, String userId, String texto) {
}
