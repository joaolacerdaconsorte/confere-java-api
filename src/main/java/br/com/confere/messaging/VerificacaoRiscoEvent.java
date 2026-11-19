package br.com.confere.messaging;

import br.com.confere.domain.enums.Veredito;

public record VerificacaoRiscoEvent(
        Long verificacaoId,
        Long usuarioId,
        Veredito veredito,
        Integer score,
        String mensagemGuardiao
) {
}
