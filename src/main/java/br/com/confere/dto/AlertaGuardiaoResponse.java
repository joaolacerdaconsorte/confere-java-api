package br.com.confere.dto;

import br.com.confere.domain.enums.CanalEnvioAlerta;
import br.com.confere.domain.enums.StatusAlerta;
import br.com.confere.domain.enums.Veredito;

import java.time.OffsetDateTime;

public record AlertaGuardiaoResponse(
        Long id,
        Long idVerificacao,
        String nomeProtegido,
        Veredito veredito,
        Integer score,
        String mensagemGuardiao,
        CanalEnvioAlerta canalEnvio,
        StatusAlerta status,
        OffsetDateTime dtEnvio,
        OffsetDateTime dtLeitura
) {
}
