package com.devweb.worker.model;

public record RespostaMessage(String correlationId, String userId, String texto, boolean erro) {
}
