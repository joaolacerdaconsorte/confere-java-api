package br.com.confere.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LeitorImagemService {

    private static final String SEM_TEXTO = "SEM_TEXTO";

    private final ChatClient chatClient;
    private final String llmApiKey;
    private final long timeoutSegundos;

    public LeitorImagemService(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${spring.ai.openai.api-key:disabled}") String llmApiKey,
            @Value("${confere.ocr.timeout-seconds:20}") long timeoutSegundos
    ) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder != null ? builder.build() : null;
        this.llmApiKey = llmApiKey;
        this.timeoutSegundos = timeoutSegundos;
    }

    public String lerTexto(byte[] imagem) {
        if (imagem == null || imagem.length == 0) {
            return "";
        }
        if (chatClient == null || !isLlmConfigured()) {
            log.warn("Leitura de imagem indisponível: LLM_API_KEY não configurada.");
            return "";
        }

        MimeType tipo = detectarTipo(imagem);
        long inicio = System.currentTimeMillis();
        try {
            String resposta = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                            .system("""
                                    Você é o leitor de imagens do aplicativo Confere.
                                    Transcreva exatamente todo o texto visível na imagem, na ordem de leitura.
                                    Inclua links, números de telefone, valores e nomes como aparecem.
                                    Não explique, não resuma, não traduza e não comente.
                                    Se não houver texto legível, responda apenas SEM_TEXTO.
                                    """)
                            .user(u -> u.text("Transcreva o texto desta imagem.")
                                    .media(tipo, new ByteArrayResource(imagem)))
                            .call()
                            .content())
                    .get(timeoutSegundos, TimeUnit.SECONDS);

            String texto = limpar(resposta);
            log.info("Imagem lida pelo Gemini: {} bytes ({}), {} caracteres em {} ms",
                    imagem.length, tipo, texto.length(), System.currentTimeMillis() - inicio);
            return texto;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception ex) {
            log.warn("Falha ao ler o texto da imagem com o Gemini após {} ms ({}).",
                    System.currentTimeMillis() - inicio, ex.toString());
            return "";
        }
    }

    private String limpar(String resposta) {
        if (resposta == null) {
            return "";
        }
        String texto = resposta.strip()
                .replaceAll("^```[a-zA-Z]*\\s*", "")
                .replaceAll("\\s*```$", "")
                .strip();
        return SEM_TEXTO.equalsIgnoreCase(texto) ? "" : texto;
    }

    private MimeType detectarTipo(byte[] imagem) {
        if (imagem.length > 3 && (imagem[0] & 0xFF) == 0x89 && imagem[1] == 'P' && imagem[2] == 'N' && imagem[3] == 'G') {
            return MimeTypeUtils.IMAGE_PNG;
        }
        if (imagem.length > 11 && imagem[0] == 'R' && imagem[1] == 'I' && imagem[2] == 'F' && imagem[3] == 'F'
                && imagem[8] == 'W' && imagem[9] == 'E' && imagem[10] == 'B' && imagem[11] == 'P') {
            return MimeType.valueOf("image/webp");
        }
        if (imagem.length > 2 && imagem[0] == 'G' && imagem[1] == 'I' && imagem[2] == 'F') {
            return MimeTypeUtils.IMAGE_GIF;
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private boolean isLlmConfigured() {
        return llmApiKey != null
                && !llmApiKey.isBlank()
                && !"disabled".equalsIgnoreCase(llmApiKey)
                && !llmApiKey.startsWith("sk-troque");
    }
}
