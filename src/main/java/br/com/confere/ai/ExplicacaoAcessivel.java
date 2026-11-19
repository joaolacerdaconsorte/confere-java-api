package br.com.confere.ai;

import java.util.List;

/**
 * Saída estruturada do Spring AI (Seção 9, requisito 04) obedecendo à RN05.
 */
public record ExplicacaoAcessivel(
        List<String> frases,
        String roteiroAudio,
        String mensagemGuardiao,
        String proximoPasso
) {
}
