package br.com.confere.client;

public record AnaliseIaRequest(
        String texto,
        String imagemBase64,
        String canal
) {
}
