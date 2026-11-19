package br.com.confere.dto;

import jakarta.validation.constraints.*;

public record TipoGolpeRequest(
        @NotBlank(message = "Código é obrigatório")
        @Size(max = 30)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Código deve conter apenas letras maiúsculas, números e underline")
        String codigo,

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 80)
        String nome,

        @Size(max = 500)
        String descricao,

        @NotNull(message = "Gravidade é obrigatória")
        @Min(value = 1, message = "Gravidade mínima é 1")
        @Max(value = 5, message = "Gravidade máxima é 5")
        Integer gravidade,

        @Size(max = 60)
        String orgaoImitado
) {
}
