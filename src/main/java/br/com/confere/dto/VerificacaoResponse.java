package br.com.confere.dto;

import br.com.confere.domain.enums.ModoVerificacao;
import br.com.confere.domain.enums.StatusVerificacao;
import br.com.confere.domain.enums.Veredito;

import java.time.OffsetDateTime;
import java.util.List;

public record VerificacaoResponse(
        Long id,
        Veredito veredito,
        Integer score,
        StatusVerificacao status,
        TipoGolpeResumoDto tipoGolpe,
        List<String> frases,
        String roteiroAudio,
        String proximoPasso,
        List<SinalDto> sinais,
        List<FonteDto> fontes,
        int guardioesAvisados,
        OffsetDateTime criadoEm,
        ModoVerificacao modo,
        String conteudoMascarado
) {
}
