package br.com.confere.dto;

import br.com.confere.dto.validation.TelefoneBR;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarPerfilRequest(
        @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres")
        String nome,

        @TelefoneBR
        String telefone,

        @Past(message = "Data de nascimento deve estar no passado")
        LocalDate dtNascimento,

        Integer codIbgeMunicipio,
        Boolean fonteGrande,
        Boolean audioAtivo
) {
}
