package com.devweb.gateway.sse;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class SseController {

    private final SseRegistry registry;

    public SseController(SseRegistry registry) {
        this.registry = registry;
    }

    /** O stream é vinculado ao usuário do JWT: ninguém recebe a resposta de outra pessoa. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        return registry.registrar(auth.getName());
    }
}
