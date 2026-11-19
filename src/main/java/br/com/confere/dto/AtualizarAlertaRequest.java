package br.com.confere.dto;

import br.com.confere.domain.enums.StatusAlerta;
import jakarta.validation.constraints.NotNull;

public record AtualizarAlertaRequest(
        @NotNull(message = "Status é obrigatório")
        StatusAlerta status
) {
}
