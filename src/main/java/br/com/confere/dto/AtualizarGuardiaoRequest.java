package br.com.confere.dto;

import br.com.confere.domain.enums.NivelMinimoAlerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarGuardiaoRequest(
        @NotBlank(message = "Parentesco é obrigatório")
        @Size(max = 30)
        String parentesco,

        @NotNull(message = "Nível mínimo é obrigatório")
        NivelMinimoAlerta nivelMinimo
) {
}
