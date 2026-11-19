package br.com.confere.dto;

import br.com.confere.domain.enums.ModoVerificacao;
import br.com.confere.domain.enums.TipoEntrada;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarVerificacaoRequest(
        @NotNull(message = "Tipo de entrada é obrigatório")
        TipoEntrada tipoEntrada,

        @Size(max = 4000, message = "Conteúdo excede o limite de 4000 caracteres")
        String conteudo,

        String canal,

        ModoVerificacao modo
) {
}
