package br.com.confere.controller;

import br.com.confere.dto.AtualizarPerfilRequest;
import br.com.confere.dto.PushTokenRequest;
import br.com.confere.dto.UsuarioResumoDto;
import br.com.confere.security.UsuarioPrincipal;
import br.com.confere.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CIDADAO', 'ADMIN')")
@Tag(name = "Usuários", description = "Perfil, preferências de acessibilidade e token de push")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/me")
    @Operation(summary = "Consultar perfil e preferências de acessibilidade")
    public ResponseEntity<UsuarioResumoDto> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(usuarioService.buscarPerfil(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar perfil e preferências de acessibilidade")
    public ResponseEntity<UsuarioResumoDto> atualizarMe(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody AtualizarPerfilRequest request
    ) {
        return ResponseEntity.ok(usuarioService.atualizarPerfil(principal.getId(), request));
    }

    @PutMapping("/me/push-token")
    @Operation(summary = "Registrar ou atualizar Expo Push Token do aparelho")
    public ResponseEntity<Void> atualizarPushToken(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody PushTokenRequest request
    ) {
        usuarioService.atualizarPushToken(principal.getId(), request);
        return ResponseEntity.noContent().build();
    }
}
