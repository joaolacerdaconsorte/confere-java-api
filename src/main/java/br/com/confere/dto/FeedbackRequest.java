package br.com.confere.dto;

import br.com.confere.domain.enums.DesfechoFeedback;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FeedbackRequest(
        @NotNull(message = "foiUtil é obrigatório")
        Boolean foiUtil,

        DesfechoFeedback desfecho,

        @DecimalMin(value = "0.00", message = "Valor evitado não pode ser negativo")
        BigDecimal valorEvitado
) {
}
