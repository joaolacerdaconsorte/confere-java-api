package br.com.confere.controller;

import br.com.confere.dto.*;
import br.com.confere.service.CatalogoAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administração de Catálogos", description = "CRUD de tipos de golpe e regras do modo offline (ADMIN)")
public class AdminCatalogoController {

    private final CatalogoAdminService catalogoAdminService;

    @GetMapping("/tipos-golpe")
    @Operation(summary = "Listar tipos de golpe paginados")
    public ResponseEntity<PaginaResponse<TipoGolpeDto>> listarTiposGolpe(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(catalogoAdminService.listarTiposGolpePaginado(pagina, tamanho));
    }

    @GetMapping("/tipos-golpe/{id}")
    @Operation(summary = "Buscar tipo de golpe por ID")
    public ResponseEntity<TipoGolpeDto> buscarTipoGolpe(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoAdminService.buscarTipoGolpe(id));
    }

    @PostMapping("/tipos-golpe")
    @Operation(summary = "Criar tipo de golpe")
    public ResponseEntity<TipoGolpeDto> criarTipoGolpe(@Valid @RequestBody TipoGolpeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoAdminService.criarTipoGolpe(request));
    }

    @PutMapping("/tipos-golpe/{id}")
    @Operation(summary = "Atualizar tipo de golpe")
    public ResponseEntity<TipoGolpeDto> atualizarTipoGolpe(
            @PathVariable Long id,
            @Valid @RequestBody TipoGolpeRequest request
    ) {
        return ResponseEntity.ok(catalogoAdminService.atualizarTipoGolpe(id, request));
    }

    @DeleteMapping("/tipos-golpe/{id}")
    @Operation(summary = "Excluir tipo de golpe")
    public ResponseEntity<Void> excluirTipoGolpe(@PathVariable Long id) {
        catalogoAdminService.excluirTipoGolpe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/regras-offline")
    @Operation(summary = "Listar regras offline paginadas")
    public ResponseEntity<PaginaResponse<RegraOfflineDto>> listarRegrasOffline(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho
    ) {
        return ResponseEntity.ok(catalogoAdminService.listarTodasRegrasOffline(pagina, tamanho));
    }

    @GetMapping("/regras-offline/{id}")
    @Operation(summary = "Buscar regra offline por ID")
    public ResponseEntity<RegraOfflineDto> buscarRegraOffline(@PathVariable Long id) {
        return ResponseEntity.ok(catalogoAdminService.buscarRegraOffline(id));
    }

    @PostMapping("/regras-offline")
    @Operation(summary = "Criar regra offline (incrementa versão)")
    public ResponseEntity<RegraOfflineDto> criarRegraOffline(@Valid @RequestBody RegraOfflineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoAdminService.criarRegraOffline(request));
    }

    @PutMapping("/regras-offline/{id}")
    @Operation(summary = "Atualizar regra offline (incrementa versão)")
    public ResponseEntity<RegraOfflineDto> atualizarRegraOffline(
            @PathVariable Long id,
            @Valid @RequestBody RegraOfflineRequest request
    ) {
        return ResponseEntity.ok(catalogoAdminService.atualizarRegraOffline(id, request));
    }

    @DeleteMapping("/regras-offline/{id}")
    @Operation(summary = "Excluir regra offline")
    public ResponseEntity<Void> excluirRegraOffline(@PathVariable Long id) {
        catalogoAdminService.excluirRegraOffline(id);
        return ResponseEntity.noContent().build();
    }
}
