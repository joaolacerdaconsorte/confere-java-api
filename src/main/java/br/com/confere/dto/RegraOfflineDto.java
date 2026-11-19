package br.com.confere.dto;

import java.math.BigDecimal;

public record RegraOfflineDto(
        Long id,
        String padrao,
        String tipoSinal,
        BigDecimal peso,
        String mensagem,
        Integer versao,
        boolean ativo
) {
}
