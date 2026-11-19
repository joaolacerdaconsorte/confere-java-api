package br.com.confere.dto;

import br.com.confere.dto.validation.TelefoneBR;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegistrarRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter no mínimo 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Senha deve conter ao menos uma letra e um número")
        String senha,

        @TelefoneBR
        String telefone,

        @Past(message = "Data de nascimento deve estar no passado")
        LocalDate dtNascimento,

        @NotNull(message = "Código IBGE do município é obrigatório")
        Integer codIbgeMunicipio
) {
}
