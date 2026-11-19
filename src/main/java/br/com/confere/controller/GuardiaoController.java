package br.com.confere.controller;

import br.com.confere.domain.enums.StatusAlerta;
import br.com.confere.dto.*;
import br.com.confere.security.UsuarioPrincipal;
import br.com.confere.service.GuardiaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CIDADAO', 'ADMIN')")
@Tag(name = "Rede de Guardiões e Alertas", description = "Convites, vínculos familiares e alertas de risco")
public class GuardiaoController {

    private final GuardiaoService guardiaoService;

    @GetMapping("/guardioes")
    @Operation(summary = "Listar meus guardiões")
    public ResponseEntity<List<VinculoGuardiaoResponse>> listarMeusGuardioes(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(guardiaoService.listarMeusGuardioes(principal.getId()));
    }

    @PostMapping("/guardioes")
    @Operation(summary = "Convidar familiar como guardião por e-mail (RN10)")
    public ResponseEntity<VinculoGuardiaoResponse> convidar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConvidarGuardiaoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardiaoService.convidar(principal.getId(), request));
    }

    @PutMapping("/guardioes/{idVinculo}")
    @Operation(summary = "Editar parentesco e nível mínimo do guardião")
    public ResponseEntity<VinculoGuardiaoResponse> atualizar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idVinculo,
            @Valid @RequestBody AtualizarGuardiaoRequest request
    ) {
        return ResponseEntity.ok(guardiaoService.atualizar(principal.getId(), idVinculo, request));
    }

    @DeleteMapping("/guardioes/{idVinculo}")
    @Operation(summary = "Revogar vínculo com guardião (RN10)")
    public ResponseEntity<Void> revogar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idVinculo
    ) {
        guardiaoService.revogar(principal.getId(), idVinculo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/guardioes/convites")
    @Operation(summary = "Listar convites pendentes recebidos")
    public ResponseEntity<List<VinculoGuardiaoResponse>> convitesRecebidos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(guardiaoService.listarConvitesRecebidos(principal.getId()));
    }

    @PostMapping("/guardioes/{idVinculo}/aceitar")
    @Operation(summary = "Aceitar convite de guardião (RN10)")
    public ResponseEntity<VinculoGuardiaoResponse> aceitar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idVinculo
    ) {
        return ResponseEntity.ok(guardiaoService.responderConvite(principal.getId(), idVinculo, true));
    }

    @PostMapping("/guardioes/{idVinculo}/recusar")
    @Operation(summary = "Recusar convite de guardião (RN10)")
    public ResponseEntity<VinculoGuardiaoResponse> recusar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long idVinculo
    ) {
        return ResponseEntity.ok(guardiaoService.responderConvite(principal.getId(), idVinculo, false));
    }

    @GetMapping("/protegidos")
    @Operation(summary = "Listar pessoas que eu protejo (vínculos ATIVOS)")
    public ResponseEntity<List<VinculoGuardiaoResponse>> protegidos(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(guardiaoService.listarProtegidos(principal.getId()));
    }

    @GetMapping("/alertas")
    @Operation(summary = "Listar alertas paginados dos meus protegidos (RN07)")
    public ResponseEntity<PaginaResponse<AlertaGuardiaoResponse>> alertas(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) StatusAlerta status,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(guardiaoService.listarAlertas(principal.getId(), status, pagina, tamanho));
    }

    @PatchMapping("/alertas/{id}")
    @Operation(summary = "Marcar alerta como LIDO ou RESOLVIDO")
    public ResponseEntity<AlertaGuardiaoResponse> atualizarAlerta(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AtualizarAlertaRequest request
    ) {
        return ResponseEntity.ok(guardiaoService.atualizarStatusAlerta(principal.getId(), id, request));
    }
}
