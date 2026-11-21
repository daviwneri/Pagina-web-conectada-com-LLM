package com.devweb.gateway.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Armazena usuários em memória (suficiente para o trabalho; troque por JPA se quiser persistir). */
@Service
public class UserService {

    private final Map<String, String> usuarios = new ConcurrentHashMap<>();
    private final PasswordEncoder encoder;

    public UserService(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public boolean registrar(String username, String senha) {
        return usuarios.putIfAbsent(username, encoder.encode(senha)) == null;
    }

    public boolean autenticar(String username, String senha) {
        String hash = usuarios.get(username);
        return hash != null && encoder.matches(senha, hash);
    }
}
