package br.com.confere.dto;

import br.com.confere.domain.enums.NivelMinimoAlerta;
import br.com.confere.domain.enums.StatusVinculo;

import java.time.OffsetDateTime;

public record VinculoGuardiaoResponse(
        Long id,
        PessoaResumoDto guardiao,
        PessoaResumoDto protegido,
        String parentesco,
        NivelMinimoAlerta nivelMinimo,
        StatusVinculo status,
        OffsetDateTime criadoEm,
        OffsetDateTime aceitoEm
) {
}
