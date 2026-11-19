package br.com.confere;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConfereApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String email, String senha) throws Exception {
        String body = """
                { "email": "%s", "senha": "%s" }
                """.formatted(email, senha);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void healthCheckDeveSerPublicoERetornar200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void deveRegistrarELogarComJwtEValidarTelefoneBR() throws Exception {
        String invalidBody = """
                {
                  "nome": "Carlos",
                  "email": "carlos@confere.app",
                  "senha": "123",
                  "telefone": "999",
                  "dtNascimento": "1980-05-10",
                  "codIbgeMunicipio": 3550308
                }
                """;

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros").isArray());

        String validBody = """
                {
                  "nome": "Carlos Silva",
                  "email": "carlos@confere.app",
                  "senha": "Senha@123",
                  "telefone": "11988887777",
                  "dtNascimento": "1980-05-10",
                  "codIbgeMunicipio": 3550308
                }
                """;

        mockMvc.perform(post("/api/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("carlos@confere.app"));

        // RN02: usuário novo sem consentimento ANALISE_IA recebe 403 CONSENTIMENTO_PENDENTE
        String tokenCarlos = loginAndGetToken("carlos@confere.app", "Senha@123");
        mockMvc.perform(post("/api/verificacoes")
                        .header("Authorization", "Bearer " + tokenCarlos)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoEntrada": "TEXTO",
                                  "conteudo": "INSS: sua prova de vida vence hoje, envie uma selfie.",
                                  "canal": "WHATSAPP",
                                  "modo": "ONLINE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("CONSENTIMENTO_PENDENTE"));
    }

    @Test
    void fluxoCompletoVerificacaoGolpeFeedbackEExclusaoRn01Rn05Rn09() throws Exception {
        String tokenCida = loginAndGetToken("cida@confere.app", "Senha@123");

        String reqVerif = """
                {
                  "tipoEntrada": "TEXTO",
                  "conteudo": "INSS: CPF 123.456.789-00 sua prova de vida vence hoje. Envie uma selfie para não perder o benefício.",
                  "canal": "WHATSAPP",
                  "modo": "ONLINE"
                }
                """;

        MvcResult res = mockMvc.perform(post("/api/verificacoes")
                        .header("Authorization", "Bearer " + tokenCida)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqVerif))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.veredito").value("GOLPE"))
                .andExpect(jsonPath("$.status").value("PROCESSADA"))
                .andExpect(jsonPath("$.guardioesAvisados").value(1))
                .andExpect(jsonPath("$.frases.length()").value(3))
                .andReturn();

        JsonNode verifJson = objectMapper.readTree(res.getResponse().getContentAsString());
        long idVerificacao = verifJson.get("id").asLong();
        assertThat(verifJson.get("conteudoMascarado").asText()).contains("[CPF]").doesNotContain("123.456.789-00");

        // RN09: outro usuário não acessa a verificação da Cida
        String tokenJoao = loginAndGetToken("joao@confere.app", "Senha@123");
        mockMvc.perform(get("/api/verificacoes/" + idVerificacao)
                        .header("Authorization", "Bearer " + tokenJoao))
                .andExpect(status().isNotFound());

        // Feedback
        mockMvc.perform(put("/api/verificacoes/" + idVerificacao + "/feedback")
                        .header("Authorization", "Bearer " + tokenCida)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "foiUtil": true, "desfecho": "NAO_PAGUEI", "valorEvitado": 1500.00 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desfecho").value("NAO_PAGUEI"));

        // RN09: exclusão física em cascata
        mockMvc.perform(delete("/api/verificacoes/" + idVerificacao)
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/verificacoes/" + idVerificacao)
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isNotFound());
    }

    @Test
    void modoOfflineDeveRetornar202ERegrasOffline304QuandoMesmaVersao() throws Exception {
        String tokenCida = loginAndGetToken("cida@confere.app", "Senha@123");

        mockMvc.perform(post("/api/verificacoes")
                        .header("Authorization", "Bearer " + tokenCida)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoEntrada": "TEXTO",
                                  "conteudo": "Urgente: envie sua senha do banco agora mesmo.",
                                  "canal": "SMS",
                                  "modo": "OFFLINE"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.modo").value("OFFLINE"));

        mockMvc.perform(get("/api/regras-offline?versao=1")
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isNotModified());
    }

    @Test
    void rn10_conviteDuplicadoOuAutoConviteDeveFalhar() throws Exception {
        String tokenCida = loginAndGetToken("cida@confere.app", "Senha@123");

        // Auto-convite -> 400
        mockMvc.perform(post("/api/guardioes")
                        .header("Authorization", "Bearer " + tokenCida)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "emailGuardiao": "cida@confere.app", "parentesco": "Eu mesma", "nivelMinimo": "GOLPE" }
                                """))
                .andExpect(status().isBadRequest());

        // Já existe vínculo ATIVO com joao@confere.app -> 409 GUARDIAO_DUPLICADO
        mockMvc.perform(post("/api/guardioes")
                        .header("Authorization", "Bearer " + tokenCida)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "emailGuardiao": "joao@confere.app", "parentesco": "Filho", "nivelMinimo": "GOLPE" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("GUARDIAO_DUPLICADO"));
    }

    @Test
    void rbacAdminDeveBloquearCidadaoEPermitirAdmin() throws Exception {
        String tokenCida = loginAndGetToken("cida@confere.app", "Senha@123");
        String tokenAdmin = loginAndGetToken("admin@confere.app", "Senha@123");

        String novoGolpe = """
                {
                  "codigo": "GOLPE_TESTE_QA",
                  "nome": "Golpe de teste automatizado",
                  "descricao": "Descrição de teste",
                  "gravidade": 4,
                  "orgaoImitado": "Correios"
                }
                """;

        mockMvc.perform(post("/api/admin/tipos-golpe")
                        .header("Authorization", "Bearer " + tokenCida)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoGolpe))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        MvcResult created = mockMvc.perform(post("/api/admin/tipos-golpe")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novoGolpe))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("GOLPE_TESTE_QA"))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/admin/tipos-golpe/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
    }

    @Test
    void errosDeParametroERotaInexistenteDevemSeguirProblemDetail() throws Exception {
        String tokenCida = loginAndGetToken("cida@confere.app", "Senha@123");

        mockMvc.perform(get("/api/verificacoes")
                        .param("veredito", "NAO_EXISTE")
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("veredito"));

        mockMvc.perform(get("/api/verificacoes/abc")
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));

        mockMvc.perform(get("/api/rota-que-nao-existe")
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    void verificacaoMultipartDeveAceitarDadosComoTextoEValidarCampos() throws Exception {
        String tokenCida = loginAndGetToken("cida@confere.app", "Senha@123");

        MockMultipartFile dadosValidos = new MockMultipartFile("dados", "", "text/plain",
                """
                { "tipoEntrada": "TEXTO", "conteudo": "Seu cartão foi bloqueado, informe a senha urgente para liberar.", "canal": "SMS", "modo": "ONLINE" }
                """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/verificacoes")
                        .file(dadosValidos)
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROCESSADA"));

        MockMultipartFile dadosSemTipo = new MockMultipartFile("dados", "", "application/json",
                """
                { "conteudo": "Mensagem sem o tipo de entrada informado." }
                """.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/verificacoes")
                        .file(dadosSemTipo)
                        .header("Authorization", "Bearer " + tokenCida))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"))
                .andExpect(jsonPath("$.erros[0].campo").value("tipoEntrada"));
    }
}
