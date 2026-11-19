package br.com.confere.dto;

import br.com.confere.domain.enums.NivelMinimoAlerta;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConvidarGuardiaoRequest(
        @NotBlank(message = "E-mail do guardião é obrigatório")
        @Email(message = "E-mail inválido")
        String emailGuardiao,

        @NotBlank(message = "Parentesco é obrigatório")
        @Size(max = 30, message = "Parentesco deve ter no máximo 30 caracteres")
        String parentesco,

        NivelMinimoAlerta nivelMinimo
) {
}
