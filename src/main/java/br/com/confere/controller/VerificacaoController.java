package br.com.confere.controller;

import br.com.confere.domain.enums.ModoVerificacao;
import br.com.confere.domain.enums.Veredito;
import br.com.confere.dto.*;
import br.com.confere.security.UsuarioPrincipal;
import br.com.confere.service.VerificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/verificacoes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CIDADAO', 'ADMIN')")
@Tag(name = "Verificações", description = "Análise inteligente de golpes/direitos, histórico paginado e feedback")
public class VerificacaoController {

    private final VerificacaoService verificacaoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enviar texto ou link para verificação (JSON)")
    public ResponseEntity<VerificacaoResponse> criarJson(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestBody CriarVerificacaoRequest request
    ) {
        VerificacaoResponse response = verificacaoService.criar(principal.getId(), request, null);
        HttpStatus status = response.modo() == ModoVerificacao.OFFLINE ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enviar print/foto com metadados para verificação (multipart)")
    public ResponseEntity<VerificacaoResponse> criarMultipart(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @Valid @RequestPart("dados") CriarVerificacaoRequest dados,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem
    ) {
        VerificacaoResponse response = verificacaoService.criar(principal.getId(), dados, imagem);
        HttpStatus status = response.modo() == ModoVerificacao.OFFLINE ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar histórico paginado de verificações do usuário (RN09)")
    public ResponseEntity<PaginaResponse<VerificacaoResponse>> listar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) Veredito veredito,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(verificacaoService.listar(principal.getId(), veredito, pagina, tamanho));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar verificação do próprio usuário (RN09)")
    public ResponseEntity<VerificacaoResponse> detalhar(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(verificacaoService.detalhar(principal.getId(), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir verificação e dados relacionados em cascata (RN09)")
    public ResponseEntity<Void> excluir(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id
    ) {
        verificacaoService.excluir(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/feedback")
    @Operation(summary = "Criar ou atualizar feedback do desfecho da verificação")
    public ResponseEntity<FeedbackResponse> feedback(
            @AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody FeedbackRequest request
    ) {
        return ResponseEntity.ok(verificacaoService.salvarFeedback(principal.getId(), id, request));
    }
}
