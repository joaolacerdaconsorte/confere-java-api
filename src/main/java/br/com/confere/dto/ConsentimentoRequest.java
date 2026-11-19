package br.com.confere.dto;

import br.com.confere.domain.enums.FinalidadeConsentimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConsentimentoRequest(
        @NotNull(message = "Finalidade é obrigatória")
        FinalidadeConsentimento finalidade,

        @NotBlank(message = "Versão do termo é obrigatória")
        @Size(max = 10)
        String versaoTermo,

        @NotNull(message = "Aceite é obrigatório")
        Boolean aceito
) {
}
