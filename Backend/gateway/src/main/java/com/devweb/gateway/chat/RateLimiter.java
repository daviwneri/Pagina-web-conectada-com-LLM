package com.devweb.gateway.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Limite simples por usuário (janela deslizante) para proteger o custo da LLM. */
@Component
public class RateLimiter {

    private final int max;
    private final long janelaMs;
    private final Map<String, Deque<Long>> historico = new ConcurrentHashMap<>();

    public RateLimiter(@Value("${app.rate-limit.max-perguntas}") int max,
                       @Value("${app.rate-limit.janela-segundos}") long janelaSegundos) {
        this.max = max;
        this.janelaMs = janelaSegundos * 1000;
    }

    public boolean permitir(String userId) {
        long agora = System.currentTimeMillis();
        Deque<Long> fila = historico.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() > janelaMs) {
                fila.pollFirst();
            }
            if (fila.size() >= max) {
                return false;
            }
            fila.addLast(agora);
            return true;
        }
    }
}
