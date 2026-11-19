package br.com.confere.client;

import br.com.confere.ai.LeitorImagemService;
import br.com.confere.service.MascaramentoService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AnaliseIaClientTest {

    private static final String MENSAGEM_INSS = "INSS Benefício liberado! Você tem direito a um saque extra do INSS "
            + "no valor de até R$ 1.412,00. Acesse agora... https://meu.inss-beneficio-gov.com.br/ Prazo limitado!";

    private static final ObjectProvider<ChatClient.Builder> SEM_LLM = new ObjectProvider<>() {
        @Override public ChatClient.Builder getObject(Object... args) throws BeansException { return null; }
        @Override public ChatClient.Builder getIfAvailable() throws BeansException { return null; }
        @Override public ChatClient.Builder getIfUnique() throws BeansException { return null; }
        @Override public ChatClient.Builder getObject() throws BeansException { return null; }
    };

    private static class LeitorFalso extends LeitorImagemService {
        private final String textoLido;
        private final AtomicReference<byte[]> recebido = new AtomicReference<>();

        LeitorFalso(String textoLido) {
            super(SEM_LLM, "disabled", 5);
            this.textoLido = textoLido;
        }

        @Override
        public String lerTexto(byte[] imagem) {
            recebido.set(imagem);
            return textoLido;
        }
    }

    private AnaliseIaClient cliente(LeitorImagemService leitor) {
        return new AnaliseIaClient("http://localhost:1", "teste", 2, new MascaramentoService(), leitor);
    }

    @Test
    void mensagemComSiteFalsoDoInssDeveVirarGolpeComOsSinais() {
        AnaliseIaResponse resposta = cliente(new LeitorFalso("")).analisar(MENSAGEM_INSS, null, "WHATSAPP");

        assertThat(resposta.veredito()).isEqualTo("GOLPE");
        assertThat(resposta.score()).isGreaterThanOrEqualTo(70);
        assertThat(resposta.tipoGolpeCodigo()).isEqualTo("FALSO_REEMBOLSO_INSS");
        assertThat(resposta.sinais()).extracting(AnaliseIaResponse.SinalIa::tipo)
                .contains("LINK_SUSPEITO", "URGENCIA", "ORGAO_OFICIAL_CITADO");
    }

    @Test
    void imagemSemTextoDeveSerLidaPeloGeminiQuandoOServicoDeIaNaoResponde() {
        LeitorFalso leitor = new LeitorFalso(MENSAGEM_INSS);
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        String base64 = Base64.getEncoder().encodeToString(jpeg);

        AnaliseIaResponse resposta = cliente(leitor).analisar("", base64, "WHATSAPP");

        assertThat(leitor.recebido.get()).isEqualTo(jpeg);
        assertThat(resposta.veredito()).isEqualTo("GOLPE");
        assertThat(resposta.textoMascarado()).contains("inss-beneficio-gov.com.br");
    }

    @Test
    void imagemComPrefixoDataUrlDeveChegarDecodificadaAoLeitor() {
        LeitorFalso leitor = new LeitorFalso(MENSAGEM_INSS);
        byte[] bytes = "imagem-de-teste".getBytes(StandardCharsets.UTF_8);
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);

        cliente(leitor).analisar(null, dataUrl, "WHATSAPP");

        assertThat(leitor.recebido.get()).isEqualTo(bytes);
    }

    @Test
    void imagemSemTextoLegivelDeveFicarInconclusiva() {
        String base64 = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        AnaliseIaResponse resposta = cliente(new LeitorFalso("")).analisar("", base64, "WHATSAPP");

        assertThat(resposta.veredito()).isEqualTo("INCONCLUSIVO");
        assertThat(resposta.score()).isZero();
    }

    @Test
    void linkOficialDoGovBrNaoDeveSerMarcadoComoSuspeito() {
        String oficial = "INSS informa: consulte o extrato do seu benefício em https://meu.inss.gov.br ou ligue 135.";

        AnaliseIaResponse resposta = cliente(new LeitorFalso("")).analisar(oficial, null, "SMS");

        assertThat(resposta.sinais()).extracting(AnaliseIaResponse.SinalIa::tipo).doesNotContain("LINK_SUSPEITO");
        assertThat(resposta.veredito()).isNotEqualTo("GOLPE");
    }
}
