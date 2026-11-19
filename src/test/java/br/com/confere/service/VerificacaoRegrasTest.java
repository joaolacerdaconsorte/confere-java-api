package br.com.confere.service;

import br.com.confere.ai.ExplicacaoAcessivel;
import br.com.confere.ai.ExplicadorAcessivelService;
import br.com.confere.client.AnaliseIaResponse;
import br.com.confere.domain.enums.NivelMinimoAlerta;
import br.com.confere.domain.enums.Veredito;
import br.com.confere.messaging.AlertaGuardiaoListener;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VerificacaoRegrasTest {

    @Test
    void rn04_deveRetornarInconclusivoQuandoOcrAbaixoDeMeioOuTextoCurto() {
        VerificacaoService service = new VerificacaoService(
                null, null, null, null, null, null, null, null, null, null, null, null
        );

        AnaliseIaResponse ocrBaixo = new AnaliseIaResponse("GOLPE", 95, "GOLPE", "FALSA_PROVA_VIDA",
                0.42, "INSS: envie sua selfie agora mesmo", List.of(), List.of(), "");
        assertThat(service.resolverVeredito(ocrBaixo, ocrBaixo.textoMascarado())).isEqualTo(Veredito.INCONCLUSIVO);

        AnaliseIaResponse textoCurto = new AnaliseIaResponse("GOLPE", 95, "GOLPE", "FALSA_PROVA_VIDA",
                0.95, "oi tudo bem", List.of(), List.of(), "");
        assertThat(service.resolverVeredito(textoCurto, textoCurto.textoMascarado())).isEqualTo(Veredito.INCONCLUSIVO);
    }

    @Test
    void rn04_deveClassificarConformeFaixasDeScore() {
        VerificacaoService service = new VerificacaoService(
                null, null, null, null, null, null, null, null, null, null, null, null
        );
        String textoValido = "Mensagem com tamanho suficiente para ser analisada pelo sistema.";

        assertThat(service.resolverVeredito(
                new AnaliseIaResponse("GOLPE", 70, "GOLPE", null, 0.9, textoValido, List.of(), List.of(), ""), textoValido))
                .isEqualTo(Veredito.GOLPE);

        assertThat(service.resolverVeredito(
                new AnaliseIaResponse("SUSPEITO", 55, "SUSPEITO", null, 0.9, textoValido, List.of(), List.of(), ""), textoValido))
                .isEqualTo(Veredito.SUSPEITO);

        assertThat(service.resolverVeredito(
                new AnaliseIaResponse("DIREITO", 15, "DIREITO", null, 0.9, textoValido, List.of(), List.of(), ""), textoValido))
                .isEqualTo(Veredito.DIREITO);

        assertThat(service.resolverVeredito(
                new AnaliseIaResponse("OFICIAL", 10, "OFICIAL", null, 0.9, textoValido, List.of(), List.of(), ""), textoValido))
                .isEqualTo(Veredito.OFICIAL);
    }

    @Test
    void rn05_explicacaoDegradadaDeveTerAteTresFrasesSemExpressaoProibida() {
        ObjectProvider<ChatClient.Builder> emptyProvider = new ObjectProvider<>() {
            @Override public ChatClient.Builder getObject(Object... args) throws BeansException { return null; }
            @Override public ChatClient.Builder getIfAvailable() throws BeansException { return null; }
            @Override public ChatClient.Builder getIfUnique() throws BeansException { return null; }
            @Override public ChatClient.Builder getObject() throws BeansException { return null; }
        };
        ExplicadorAcessivelService explicador = new ExplicadorAcessivelService(emptyProvider, "disabled");
        AnaliseIaResponse analise = new AnaliseIaResponse("GOLPE", 94, "GOLPE", "FALSA_PROVA_VIDA",
                null, "INSS prova de vida", List.of(), List.of(), "");

        ExplicacaoAcessivel exp = explicador.gerarExplicacao(Veredito.GOLPE, analise, "Maria Aparecida");

        assertThat(exp.frases()).hasSizeLessThanOrEqualTo(3);
        String textoCompleto = String.join(" ", exp.frases()).toLowerCase();
        assertThat(textoCompleto).doesNotContain("100% seguro").doesNotContain("com certeza");
        assertThat(textoCompleto).contains("135");
    }

    @Test
    void rn07_alertaAoGuardiaoDeveRespeitarNivelMinimo() {
        assertThat(AlertaGuardiaoListener.deveAlertar(Veredito.GOLPE, NivelMinimoAlerta.GOLPE)).isTrue();
        assertThat(AlertaGuardiaoListener.deveAlertar(Veredito.GOLPE, NivelMinimoAlerta.SUSPEITO)).isTrue();
        assertThat(AlertaGuardiaoListener.deveAlertar(Veredito.SUSPEITO, NivelMinimoAlerta.GOLPE)).isFalse();
        assertThat(AlertaGuardiaoListener.deveAlertar(Veredito.SUSPEITO, NivelMinimoAlerta.SUSPEITO)).isTrue();
        assertThat(AlertaGuardiaoListener.deveAlertar(Veredito.OFICIAL, NivelMinimoAlerta.SUSPEITO)).isFalse();
    }
}
