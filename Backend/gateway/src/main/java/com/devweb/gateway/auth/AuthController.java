package com.devweb.gateway.auth;

import com.devweb.gateway.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;
    private final JwtService jwt;

    public AuthController(UserService users, JwtService jwt) {
        this.users = users;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthRequest req) {
        if (!users.registrar(req.username(), req.password())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", "Esse usuário já existe."));
        }
        return ResponseEntity.ok(Map.of("token", jwt.gerar(req.username()), "username", req.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest req) {
        if (!users.autenticar(req.username(), req.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "Usuário ou senha inválidos."));
        }
        return ResponseEntity.ok(Map.of("token", jwt.gerar(req.username()), "username", req.username()));
    }
}
