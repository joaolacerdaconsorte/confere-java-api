package br.com.confere.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * RN01: Mascaramento antes de tudo + SHA-256 do conteúdo original.
 */
@Service
public class MascaramentoService {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern CARTAO_PATTERN = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    private static final Pattern CONTA_PATTERN = Pattern.compile(
            "(?i)(ag[êe]ncia\\s*[:#]?\\s*\\d{3,5}(?:-\\d)?\\s*(?:[,;/]?\\s*)?(?:c/?c|conta)?\\s*[:#]?\\s*\\d{4,10}-?[\\dxX]?|conta\\s*[:#]?\\s*\\d{4,10}-?[\\dxX]?)"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern TELEFONE_PATTERN = Pattern.compile("(?:\\+?55\\s?)?(?:\\(?\\d{2}\\)?\\s?)?9?\\d{4}-?\\d{4}\\b");

    public String mascarar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        String resultado = CPF_PATTERN.matcher(texto).replaceAll("[CPF]");
        resultado = EMAIL_PATTERN.matcher(resultado).replaceAll("[EMAIL]");
        resultado = CONTA_PATTERN.matcher(resultado).replaceAll("[CONTA]");
        resultado = CARTAO_PATTERN.matcher(resultado).replaceAll("[CARTAO]");
        resultado = TELEFONE_PATTERN.matcher(resultado).replaceAll("[TELEFONE]");
        return resultado;
    }

    public String sha256(String conteudoOriginal) {
        byte[] bytes = (conteudoOriginal == null ? "" : conteudoOriginal).getBytes(StandardCharsets.UTF_8);
        return sha256(bytes);
    }

    public String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}
