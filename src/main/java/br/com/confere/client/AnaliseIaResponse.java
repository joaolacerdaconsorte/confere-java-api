package br.com.confere.client;

import java.math.BigDecimal;
import java.util.List;

public record AnaliseIaResponse(
        String veredito,
        int score,
        String classe,
        String tipoGolpeCodigo,
        Double ocrConfianca,
        String textoMascarado,
        List<SinalIa> sinais,
        List<FonteIa> fontes,
        String justificativa
) {
    public record SinalIa(String tipo, String trecho, BigDecimal peso) {
    }

    public record FonteIa(String titulo, String url, String trecho) {
    }
}
