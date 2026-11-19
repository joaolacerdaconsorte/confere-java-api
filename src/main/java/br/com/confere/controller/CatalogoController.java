package br.com.confere.controller;

import br.com.confere.dto.CanalOrigemDto;
import br.com.confere.dto.RegrasOfflineResponse;
import br.com.confere.dto.TipoGolpeDto;
import br.com.confere.service.CatalogoAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CIDADAO', 'ADMIN')")
@Tag(name = "Catálogos e Regras Offline", description = "Listas para o aplicativo e sincronização offline")
public class CatalogoController {

    private final CatalogoAdminService catalogoAdminService;

    @GetMapping("/catalogos/canais")
    @Operation(summary = "Listar canais de origem")
    public ResponseEntity<List<CanalOrigemDto>> listarCanais() {
        return ResponseEntity.ok(catalogoAdminService.listarCanais());
    }

    @GetMapping("/catalogos/tipos-golpe")
    @Operation(summary = "Listar tipos de golpe do catálogo")
    public ResponseEntity<List<TipoGolpeDto>> listarTiposGolpe() {
        return ResponseEntity.ok(catalogoAdminService.listarTiposGolpe());
    }

    @GetMapping("/regras-offline")
    @Operation(summary = "Obter regras ativas do modo offline (304 se versão não mudou)")
    public ResponseEntity<RegrasOfflineResponse> listarRegrasOffline(
            @RequestParam(required = false) Integer versao
    ) {
        RegrasOfflineResponse response = catalogoAdminService.listarRegrasOfflineAtivas(versao);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok(response);
    }
}
