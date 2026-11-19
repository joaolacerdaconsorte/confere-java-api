package br.com.confere.controller;

import br.com.confere.dto.ConsentimentoRequest;
import br.com.confere.dto.ConsentimentoResponse;
import br.com.confere.security.UsuarioPrincipal;
import br.com.confere.service.ConsentimentoService;
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
@RequestMapping("/api/consentimentos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CIDADAO', 'ADMIN')")
@Tag(name = "Consentimentos LGPD", description = "Consulta e registro de aceite por finalidade")
public class ConsentimentoController {

    private final ConsentimentoService consentimentoService;

    @GetMapping
    @Operation(summary = "Listar histórico de consentimentos do usuário")
    public ResponseEntity<List<ConsentimentoResponse>> listar(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return ResponseEntity.ok(consentimentoService.listar(principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Registrar aceite ou revogação de consentimento por finalidade")
    public ResponseEntity<ConsentimentoResponse> registrar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody ConsentimentoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentimentoService.registrar(principal.getId(), request));
    }
}
