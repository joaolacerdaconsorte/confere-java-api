package br.com.confere.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MascaramentoServiceTest {

    private final MascaramentoService service = new MascaramentoService();

    @Test
    void rn01_deveMascararCpfCartaoContaEmailETelefone() {
        String original = "Olá, meu CPF é 123.456.789-00, cartão 4111 2222 3333 4444, "
                + "agência 1234 conta 56789-0, email teste@exemplo.com e zap (11) 98765-4321.";

        String mascarado = service.mascarar(original);

        assertThat(mascarado)
                .contains("[CPF]")
                .contains("[CARTAO]")
                .contains("[CONTA]")
                .contains("[EMAIL]")
                .contains("[TELEFONE]")
                .doesNotContain("123.456.789-00")
                .doesNotContain("4111 2222 3333 4444")
                .doesNotContain("teste@exemplo.com")
                .doesNotContain("98765-4321");
    }

    @Test
    void rn01_deveGerarHashSha256DeterministicoCom64Caracteres() {
        String hash1 = service.sha256("INSS: sua prova de vida vence hoje");
        String hash2 = service.sha256("INSS: sua prova de vida vence hoje");

        assertThat(hash1).hasSize(64).isEqualTo(hash2);
    }
}
