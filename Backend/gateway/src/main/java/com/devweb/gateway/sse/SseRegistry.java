package com.devweb.gateway.sse;

import com.devweb.gateway.chat.RespostaMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SseRegistry {
    private static final Duration RETENCAO = Duration.ofMinutes(30);
    private final Map<String, Tarefa> tarefas = new ConcurrentHashMap<>();

    private static class Tarefa {
        final String dono;
        final Set<SseEmitter> streams = new HashSet<>();
        Instant expira = Instant.now().plus(RETENCAO);
        RespostaMessage resultado;

        Tarefa(String dono) {
            this.dono = dono;
        }

        synchronized void remover(SseEmitter emitter) {
            streams.remove(emitter);
        }
    }

    public void criar(String id, String dono) {
        tarefas.put(id, new Tarefa(dono));
    }

    public void descartar(String id) {
        tarefas.remove(id);
    }

    public SseEmitter registrar(String id, String dono) {
        Tarefa tarefa = tarefas.get(id);
        if (tarefa == null || !tarefa.dono.equals(dono)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        synchronized (tarefa) {
            if (!tarefa.expira.isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            SseEmitter emitter = new SseEmitter(0L);
            if (tarefa.resultado != null) {
                finalizar(emitter, tarefa.resultado);
            } else {
                tarefa.streams.add(emitter);
                emitter.onCompletion(() -> tarefa.remover(emitter));
                emitter.onTimeout(() -> tarefa.remover(emitter));
                emitter.onError(e -> tarefa.remover(emitter));
                try {
                    emitter.send(SseEmitter.event().name("conectado").data("ok"));
                } catch (IOException | IllegalStateException e) {
                    tarefa.remover(emitter);
                    emitter.completeWithError(e);
                }
            }
            return emitter;
        }
    }

    public void entregar(RespostaMessage resposta) {
        Tarefa tarefa = tarefas.get(resposta.correlationId());
        if (tarefa == null || !tarefa.dono.equals(resposta.userId())) {
            return;
        }
        synchronized (tarefa) {
            if (tarefa.resultado != null || !tarefa.expira.isAfter(Instant.now())) {
                return;
            }
            tarefa.resultado = resposta;
            tarefa.expira = Instant.now().plus(RETENCAO);
            for (SseEmitter emitter : Set.copyOf(tarefa.streams)) {
                finalizar(emitter, resposta);
            }
            tarefa.streams.clear();
        }
    }

    private void finalizar(SseEmitter emitter, RespostaMessage resposta) {
        try {
            emitter.send(SseEmitter.event().name("resposta").id(resposta.correlationId())
                    .data(resposta, MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException | IllegalStateException e) {
            emitter.completeWithError(e);
        }
    }

    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        tarefas.forEach((id, tarefa) -> {
            synchronized (tarefa) {
                if (!tarefa.expira.isAfter(Instant.now())) {
                    tarefas.remove(id, tarefa);
                    Set.copyOf(tarefa.streams).forEach(SseEmitter::complete);
                    return;
                }
                for (SseEmitter emitter : Set.copyOf(tarefa.streams)) {
                    try {
                        emitter.send(SseEmitter.event().comment("ping"));
                    } catch (IOException | IllegalStateException e) {
                        tarefa.remover(emitter);
                        emitter.completeWithError(e);
                    }
                }
            }
        });
    }
}
