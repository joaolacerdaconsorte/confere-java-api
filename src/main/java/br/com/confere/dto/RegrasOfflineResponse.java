package br.com.confere.dto;

import java.util.List;

public record RegrasOfflineResponse(
        Integer versao,
        List<RegraOfflineDto> regras
) {
}
