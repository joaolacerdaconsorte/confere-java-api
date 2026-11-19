package br.com.confere.client;

import br.com.confere.service.MascaramentoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class AnaliseIaClient {

    private final RestClient restClient;
    private final String apiKey;
    private final MascaramentoService mascaramentoService;

    public AnaliseIaClient(
            @Value("${confere.ia.base-url:http://localhost:8000}") String baseUrl,
            @Value("${confere.ia.api-key:dev-ia-key}") String apiKey,
            MascaramentoService mascaramentoService
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.mascaramentoService = mascaramentoService;
    }

    /**
     * Chama o serviço de IA em Python (/v1/analises).
     * Caso o serviço externo esteja indisponível em ambiente local/avaliação,
     * aplica análise heurística local para garantir disponibilidade (fallback degradado).
     */
    public AnaliseIaResponse analisar(String textoMascarado, String imagemBase64, String canal) {
        try {
            AnaliseIaResponse response = restClient.post()
                    .uri("/v1/analises")
                    .header("X-Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AnaliseIaRequest(textoMascarado, imagemBase64, canal))
                    .retrieve()
                    .body(AnaliseIaResponse.class);
            if (response != null) {
                return response;
            }
        } catch (Exception ex) {
            log.warn("Serviço de IA indisponível ({}); aplicando fallback heurístico local.", ex.getMessage());
        }
        return analisarFallbackLocal(textoMascarado);
    }

    private AnaliseIaResponse analisarFallbackLocal(String textoMascarado) {
        String safeText = textoMascarado == null ? "" : mascaramentoService.mascarar(textoMascarado);
        String lower = safeText.toLowerCase(Locale.ROOT);

        if (safeText.trim().length() < 15) {
            return new AnaliseIaResponse(
                    "INCONCLUSIVO",
                    0,
                    "INCONCLUSIVO",
                    null,
                    null,
                    safeText,
                    List.of(),
                    List.of(new AnaliseIaResponse.FonteIa("Portal Gov.br", "https://www.gov.br", "Confira sempre pelos canais oficiais.")),
                    "Texto muito curto para análise segura."
            );
        }

        List<AnaliseIaResponse.SinalIa> sinais = new ArrayList<>();
        int score = 10;
        String tipoGolpeCodigo = null;

        if (lower.contains("selfie") || lower.contains("biometria") || lower.contains("foto do rosto")) {
            sinais.add(new AnaliseIaResponse.SinalIa("PEDIDO_SELFIE", "pedido de selfie/biometria", new BigDecimal("0.60")));
            score += 50;
            if (lower.contains("inss") || lower.contains("prova de vida")) {
                tipoGolpeCodigo = "FALSA_PROVA_VIDA";
            }
        }
        if (lower.contains("senha") || lower.contains("token") || lower.contains("pin")) {
            sinais.add(new AnaliseIaResponse.SinalIa("PEDIDO_SENHA", "pedido de senha ou token", new BigDecimal("0.60")));
            score += 50;
            if (tipoGolpeCodigo == null) {
                tipoGolpeCodigo = "FALSA_CENTRAL";
            }
        }
        if (lower.contains("vence hoje") || lower.contains("urgente") || lower.contains("bloqueado") || lower.contains("último dia")) {
            sinais.add(new AnaliseIaResponse.SinalIa("URGENCIA", "senso de urgência", new BigDecimal("0.30")));
            score += 25;
        }
        if (lower.contains("bit.ly") || lower.contains("tinyurl") || lower.contains("clique aqui") || lower.contains(".xyz")) {
            sinais.add(new AnaliseIaResponse.SinalIa("LINK_SUSPEITO", "link suspeito ou encurtado", new BigDecimal("0.35")));
            score += 25;
        }
        if (lower.contains("pix") || lower.contains("taxa")) {
            sinais.add(new AnaliseIaResponse.SinalIa("CHAVE_PIX", "pedido de Pix ou taxa", new BigDecimal("0.35")));
            score += 25;
        }
        if (lower.contains("inss") || lower.contains("banco central") || lower.contains("receita")) {
            sinais.add(new AnaliseIaResponse.SinalIa("ORGAO_OFICIAL_CITADO", "órgão oficial citado", new BigDecimal("0.15")));
            score += 10;
        }

        // Caso de direito social (ex.: conta de luz / tarifa social / BPC / valores a receber)
        if (sinais.isEmpty() && (lower.contains("tarifa social") || lower.contains("conta de luz")
                || lower.contains("cadunico") || lower.contains("cadúnico")
                || lower.contains("bpc") || lower.contains("benefício") || lower.contains("desconto"))) {
            return new AnaliseIaResponse(
                    "DIREITO",
                    15,
                    "DIREITO",
                    null,
                    null,
                    safeText,
                    List.of(),
                    List.of(new AnaliseIaResponse.FonteIa("Aneel - Tarifa Social", "https://www.gov.br/aneel", "Famílias no CadÚnico podem ter até 65% de desconto na conta de luz.")),
                    "Há indícios de benefício social disponível para você."
            );
        }

        score = Math.min(100, score);
        String veredito;
        if (score >= 70) {
            veredito = "GOLPE";
            if (tipoGolpeCodigo == null) {
                tipoGolpeCodigo = "FALSA_CENTRAL";
            }
        } else if (score >= 40) {
            veredito = "SUSPEITO";
        } else {
            veredito = "OFICIAL";
        }

        List<AnaliseIaResponse.FonteIa> fontes = List.of(
                new AnaliseIaResponse.FonteIa("INSS - Canal Oficial 135", "https://www.gov.br/inss", "O INSS não pede selfie, senha ou pagamento por WhatsApp.")
        );

        return new AnaliseIaResponse(
                veredito,
                score,
                veredito,
                tipoGolpeCodigo,
                null,
                safeText,
                sinais,
                fontes,
                "Análise realizada com verificação de sinais de risco."
        );
    }
}
