package br.com.confere.messaging;

public record VerificacaoOfflineEvent(
        Long verificacaoId,
        Long usuarioId,
        String textoMascarado,
        String canalCodigo
) {
}
