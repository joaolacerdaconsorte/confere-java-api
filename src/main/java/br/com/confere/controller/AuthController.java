package br.com.confere.controller;

import br.com.confere.dto.LoginRequest;
import br.com.confere.dto.LoginResponse;
import br.com.confere.dto.RegistrarRequest;
import br.com.confere.dto.RegistrarResponse;
import br.com.confere.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Cadastro e login de cidadãos e administradores")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    @Operation(summary = "Cadastrar nova conta de cidadão")
    public ResponseEntity<RegistrarResponse> registrar(@Valid @RequestBody RegistrarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar e obter token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
