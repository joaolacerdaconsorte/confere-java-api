package br.com.confere.dto;

import br.com.confere.domain.enums.FinalidadeConsentimento;

import java.time.OffsetDateTime;

public record ConsentimentoResponse(
        Long id,
        FinalidadeConsentimento finalidade,
        String versaoTermo,
        boolean aceito,
        OffsetDateTime dtRegistro
) {
}
