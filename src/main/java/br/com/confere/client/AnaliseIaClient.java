package br.com.confere.client;

import br.com.confere.ai.LeitorImagemService;
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
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AnaliseIaClient {

    private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|www\\.)[^\\s<>\"']+");
    private static final Pattern DOMINIO_PATTERN = Pattern.compile(
            "(?i)\\b(?:[a-z0-9-]+\\.)+(?:com|net|org|info|xyz|top|online|site|club|app|link|live|shop|vip|io)(?:\\.br)?\\b");
    private static final List<String> DOMINIOS_OFICIAIS = List.of(
            "gov.br", "caixa.gov.br", "bcb.gov.br", "bb.com.br", "itau.com.br", "bradesco.com.br",
            "santander.com.br", "nubank.com.br", "caixa.com.br", "correios.com.br");
    private static final List<String> MARCAS_IMITADAS = List.of(
            "gov", "inss", "caixa", "receita", "detran", "correios", "bancocentral", "bcb", "serasa",
            "nubank", "itau", "bradesco", "santander", "bb-", "meuinss", "cadunico", "bolsa");
    private static final List<String> TERMOS_URGENCIA = List.of(
            "vence hoje", "urgente", "bloqueado", "último dia", "ultimo dia", "prazo limitado", "acesse agora",
            "imediatamente", "últimas horas", "ultimas horas", "expira", "hoje mesmo", "não perca", "nao perca",
            "será cancelado", "sera cancelado", "suspenso");
    private static final List<String> TERMOS_DINHEIRO = List.of(
            "r$", "saque", "valor de até", "valor de ate", "dinheiro esquecido", "valores a receber", "reembolso",
            "devolução", "devolucao", "indenização", "indenizacao");

    private final RestClient restClient;
    private final String apiKey;
    private final MascaramentoService mascaramentoService;
    private final LeitorImagemService leitorImagemService;

    public AnaliseIaClient(
            @Value("${confere.ia.base-url:http://localhost:8000}") String baseUrl,
            @Value("${confere.ia.api-key:dev-ia-key}") String apiKey,
            @Value("${confere.ia.timeout-seconds:20}") long timeoutSegundos,
            MascaramentoService mascaramentoService,
            LeitorImagemService leitorImagemService
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.mascaramentoService = mascaramentoService;
        this.leitorImagemService = leitorImagemService;
    }

    public AnaliseIaResponse analisar(String textoMascarado, String imagemBase64, String canal) {
        String imagemLimpa = removerPrefixoDataUrl(imagemBase64);
        boolean temImagem = imagemLimpa != null && !imagemLimpa.isBlank();
        log.info("Análise solicitada: texto com {} caracteres, imagem em base64 com {} caracteres, canal {}",
                textoMascarado == null ? 0 : textoMascarado.length(), temImagem ? imagemLimpa.length() : 0, canal);

        long inicio = System.currentTimeMillis();
        try {
            AnaliseIaResponse response = restClient.post()
                    .uri("/v1/analises")
                    .header("X-Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new AnaliseIaRequest(textoMascarado, temImagem ? imagemLimpa : null, canal))
                    .retrieve()
                    .body(AnaliseIaResponse.class);
            if (response != null && (!temImagem || temTexto(response.textoMascarado()))) {
                log.info("Serviço de IA respondeu em {} ms", System.currentTimeMillis() - inicio);
                return response;
            }
            if (response != null) {
                log.warn("Serviço de IA respondeu sem texto lido da imagem (ocrConfianca {}); lendo a imagem pelo Gemini.",
                        response.ocrConfianca());
            }
        } catch (Exception ex) {
            log.warn("Serviço de IA indisponível após {} ms ({}); aplicando a análise local.",
                    System.currentTimeMillis() - inicio, ex.getMessage());
        }

        String texto = textoMascarado;
        if (temImagem && !temTexto(textoMascarado)) {
            texto = leitorImagemService.lerTexto(decodificar(imagemLimpa));
            if (!temTexto(texto)) {
                log.warn("Nenhum texto foi lido da imagem; o resultado será INCONCLUSIVO.");
            }
        }
        return analisarFallbackLocal(texto);
    }

    private boolean temTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private String removerPrefixoDataUrl(String imagemBase64) {
        if (imagemBase64 == null) {
            return null;
        }
        int virgula = imagemBase64.indexOf(',');
        if (imagemBase64.startsWith("data:") && virgula > 0) {
            return imagemBase64.substring(virgula + 1);
        }
        return imagemBase64;
    }

    private byte[] decodificar(String imagemBase64) {
        try {
            return Base64.getMimeDecoder().decode(imagemBase64);
        } catch (IllegalArgumentException ex) {
            log.warn("Imagem em base64 inválida ({}).", ex.getMessage());
            return new byte[0];
        }
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
        String termoUrgencia = primeiroTermo(lower, TERMOS_URGENCIA);
        if (termoUrgencia != null) {
            sinais.add(new AnaliseIaResponse.SinalIa("URGENCIA", "senso de urgência: " + termoUrgencia, new BigDecimal("0.30")));
            score += 25;
        }

        String dominioFalso = dominioImitandoOrgao(safeText);
        if (dominioFalso != null) {
            sinais.add(new AnaliseIaResponse.SinalIa("LINK_SUSPEITO", "site que imita órgão ou banco: " + dominioFalso, new BigDecimal("0.45")));
            score += 40;
        } else if (lower.contains("bit.ly") || lower.contains("tinyurl") || lower.contains("clique aqui") || lower.contains(".xyz")) {
            sinais.add(new AnaliseIaResponse.SinalIa("LINK_SUSPEITO", "link suspeito ou encurtado", new BigDecimal("0.35")));
            score += 25;
        }

        if (lower.contains("pix") || lower.contains("taxa")) {
            sinais.add(new AnaliseIaResponse.SinalIa("CHAVE_PIX", "pedido de Pix ou taxa", new BigDecimal("0.35")));
            score += 25;
        }
        String termoDinheiro = primeiroTermo(lower, TERMOS_DINHEIRO);
        if (termoDinheiro != null) {
            sinais.add(new AnaliseIaResponse.SinalIa("VALOR_DINHEIRO", "promessa de dinheiro: " + termoDinheiro, new BigDecimal("0.20")));
            score += 15;
        }
        if (lower.contains("inss") || lower.contains("banco central") || lower.contains("receita")) {
            sinais.add(new AnaliseIaResponse.SinalIa("ORGAO_OFICIAL_CITADO", "órgão oficial citado", new BigDecimal("0.15")));
            score += 10;
        }

        if (tipoGolpeCodigo == null) {
            tipoGolpeCodigo = tipoGolpePorContexto(lower, dominioFalso != null, termoDinheiro != null);
        }

        boolean soSinalDeDinheiro = sinais.stream().allMatch(s -> "VALOR_DINHEIRO".equals(s.tipo()));
        if (soSinalDeDinheiro && (lower.contains("tarifa social") || lower.contains("conta de luz")
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

    private String primeiroTermo(String texto, List<String> termos) {
        return termos.stream().filter(texto::contains).findFirst().orElse(null);
    }

    private String tipoGolpePorContexto(String lower, boolean dominioFalso, boolean prometeDinheiro) {
        if (lower.contains("troquei de número") || lower.contains("troquei de numero") || lower.contains("oi mãe")
                || lower.contains("oi mae") || lower.contains("oi pai")) {
            return "FALSO_PARENTE";
        }
        if (lower.contains("inss") && prometeDinheiro) {
            return "FALSO_REEMBOLSO_INSS";
        }
        if (lower.contains("valores a receber") || lower.contains("dinheiro esquecido")) {
            return "FALSO_VALORES_RECEBER";
        }
        if (lower.contains("rastreio") || lower.contains("encomenda") || lower.contains("correios")) {
            return "LINK_RASTREIO";
        }
        if (lower.contains("boleto")) {
            return "FALSO_BOLETO";
        }
        if (dominioFalso && prometeDinheiro) {
            return "FALSO_VALORES_RECEBER";
        }
        return null;
    }

    private String dominioImitandoOrgao(String texto) {
        for (String dominio : extrairDominios(texto)) {
            boolean oficial = DOMINIOS_OFICIAIS.stream()
                    .anyMatch(o -> dominio.equals(o) || dominio.endsWith("." + o));
            if (!oficial && MARCAS_IMITADAS.stream().anyMatch(dominio::contains)) {
                return dominio;
            }
        }
        return null;
    }

    private Set<String> extrairDominios(String texto) {
        Set<String> dominios = new LinkedHashSet<>();
        Matcher url = URL_PATTERN.matcher(texto);
        while (url.find()) {
            String host = url.group().toLowerCase(Locale.ROOT)
                    .replaceFirst("^https?://", "")
                    .replaceFirst("^www\\.", "")
                    .split("[/?#:]", 2)[0]
                    .replaceAll("[.,;!)]+$", "");
            if (!host.isBlank()) {
                dominios.add(host);
            }
        }
        Matcher dominio = DOMINIO_PATTERN.matcher(texto);
        while (dominio.find()) {
            dominios.add(dominio.group().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", ""));
        }
        return dominios;
    }
}
