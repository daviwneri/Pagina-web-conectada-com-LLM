package com.devweb.gateway.sse;

import com.devweb.gateway.chat.RespostaMessage;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/** Mapa em memória userId -> emitters ativos (suporta várias abas do mesmo usuário). */
@Component
public class SseRegistry {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    // Respostas que chegaram quando o usuário estava desconectado; entregues na reconexão.
    private final Map<String, Queue<RespostaMessage>> pendentes = new ConcurrentHashMap<>();

    public SseEmitter registrar(String userId) {
        SseEmitter emitter = new SseEmitter(0L); // sem timeout; o cliente fecha quando quiser
        List<SseEmitter> lista = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);

        Runnable remover = () -> lista.remove(emitter);
        emitter.onCompletion(remover);
        emitter.onTimeout(remover);
        emitter.onError(e -> remover.run());

        try {
            // Força o envio dos headers para o cliente saber que a conexão abriu.
            emitter.send(SseEmitter.event().name("conectado").data("ok"));
            Queue<RespostaMessage> fila = pendentes.remove(userId);
            if (fila != null) {
                for (RespostaMessage r : fila) {
                    enviar(emitter, r);
                }
            }
        } catch (IOException e) {
            remover.run();
        }
        return emitter;
    }

    public void entregar(RespostaMessage resposta) {
        List<SseEmitter> lista = emitters.getOrDefault(resposta.userId(), List.of());
        boolean entregue = false;
        for (SseEmitter emitter : lista) {
            try {
                enviar(emitter, resposta);
                entregue = true;
            } catch (IOException e) {
                lista.remove(emitter);
                emitter.completeWithError(e);
            }
        }
        if (!entregue) {
            pendentes.computeIfAbsent(resposta.userId(), k -> new ConcurrentLinkedQueue<>()).add(resposta);
        }
    }

    private void enviar(SseEmitter emitter, RespostaMessage r) throws IOException {
        emitter.send(SseEmitter.event()
                .name("resposta")
                .id(r.correlationId())
                .data(r, MediaType.APPLICATION_JSON));
    }

    // Heartbeat: evita que proxies/navegadores derrubem conexões ociosas.
    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        emitters.values().forEach(lista -> {
            for (SseEmitter emitter : lista) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException e) {
                    lista.remove(emitter);
                }
            }
        });
    }
}
