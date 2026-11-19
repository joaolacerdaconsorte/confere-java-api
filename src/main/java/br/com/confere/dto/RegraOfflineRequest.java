package br.com.confere.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RegraOfflineRequest(
        @NotBlank(message = "Padrão regex é obrigatório")
        @Size(max = 300)
        String padrao,

        @NotBlank(message = "Tipo de sinal é obrigatório")
        @Size(max = 25)
        String tipoSinal,

        @NotNull(message = "Peso é obrigatório")
        @DecimalMin(value = "0.00")
        @DecimalMax(value = "1.00")
        BigDecimal peso,

        @NotBlank(message = "Mensagem é obrigatória")
        @Size(max = 300)
        String mensagem,

        Boolean ativo
) {
}
