package br.com.confere.dto;

import br.com.confere.domain.enums.DesfechoFeedback;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FeedbackResponse(
        Long id,
        boolean foiUtil,
        DesfechoFeedback desfecho,
        BigDecimal valorEvitado,
        OffsetDateTime dtRegistro
) {
}
