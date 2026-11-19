package br.com.confere.ai;

import br.com.confere.client.AnaliseIaResponse;
import br.com.confere.domain.enums.Veredito;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Serviço Spring AI que traduz a análise técnica do motor de IA em linguagem simples
 * para idosos e pessoas com baixo letramento digital (RN05).
 */
@Slf4j
@Service
public class ExplicadorAcessivelService {

    private final ChatClient chatClient;
    private final String llmApiKey;

    public ExplicadorAcessivelService(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${spring.ai.openai.api-key:disabled}") String llmApiKey
    ) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder != null ? builder.build() : null;
        this.llmApiKey = llmApiKey;
    }

    public ExplicacaoAcessivel gerarExplicacao(Veredito veredito, AnaliseIaResponse analise, String nomeUsuario) {
        if (chatClient != null && isLlmConfigured()) {
            try {
                String sinaisTexto = analise.sinais() == null ? "nenhum" :
                        analise.sinais().stream()
                                .map(s -> s.tipo() + " (" + s.trecho() + ")")
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("nenhum");

                String fonteTexto = (analise.fontes() != null && !analise.fontes().isEmpty())
                        ? analise.fontes().get(0).titulo()
                        : "Canal oficial gov.br / 135";

                ExplicacaoAcessivel resultado = chatClient.prompt()
                        .system("""
                                Você é o explicador acessível do aplicativo Confere para idosos e pessoas com baixo letramento digital.
                                Regras obrigatórias (RN05):
                                - Máximo de 3 frases curtas, simples e sem jargão técnico em 'frases'.
                                - A última frase deve sempre indicar o canal oficial (ex.: "Na dúvida, ligue 135." ou "Confirme no site gov.br.").
                                - NUNCA use as expressões "100% seguro" ou "com certeza".
                                - 'roteiroAudio' deve ser um texto curto e calmo para leitura em voz alta.
                                - 'mensagemGuardiao' deve resumir o alerta para um familiar.
                                - 'proximoPasso' deve dar uma ação prática imediata.
                                """)
                        .user(u -> u.text("""
                                Veredito: {veredito}
                                Score de risco: {score}/100
                                Tipo de golpe: {tipoGolpe}
                                Sinais detectados: {sinais}
                                Fonte oficial: {fonte}
                                Justificativa técnica: {justificativa}
                                Nome do cidadão: {nome}
                                """)
                                .param("veredito", veredito.name())
                                .param("score", analise.score())
                                .param("tipoGolpe", analise.tipoGolpeCodigo() != null ? analise.tipoGolpeCodigo() : "não identificado")
                                .param("sinais", sinaisTexto)
                                .param("fonte", fonteTexto)
                                .param("justificativa", analise.justificativa() != null ? analise.justificativa() : "")
                                .param("nome", nomeUsuario))
                        .call()
                        .entity(ExplicacaoAcessivel.class);

                if (resultado != null && resultado.frases() != null && !resultado.frases().isEmpty()) {
                    return sanitizarRn05(resultado, veredito);
                }
            } catch (Exception ex) {
                log.warn("Falha ao chamar LLM via Spring AI ({}); usando resposta degradada padrão.", ex.getMessage());
            }
        }
        return respostaPadrao(veredito, analise, nomeUsuario);
    }

    private boolean isLlmConfigured() {
        return llmApiKey != null
                && !llmApiKey.isBlank()
                && !"disabled".equalsIgnoreCase(llmApiKey)
                && !llmApiKey.startsWith("sk-troque");
    }

    private ExplicacaoAcessivel sanitizarRn05(ExplicacaoAcessivel entrada, Veredito veredito) {
        List<String> frases = entrada.frases().stream()
                .limit(3)
                .map(f -> f.replaceAll("(?i)100%\\s*seguro", "aparentemente oficial")
                        .replaceAll("(?i)com\\s+certeza", "com base nos sinais"))
                .toList();

        return new ExplicacaoAcessivel(
                frases,
                entrada.roteiroAudio(),
                entrada.mensagemGuardiao(),
                entrada.proximoPasso()
        );
    }

    public ExplicacaoAcessivel respostaPadrao(Veredito veredito, AnaliseIaResponse analise, String nomeUsuario) {
        String primeiroNome = nomeUsuario != null && !nomeUsuario.isBlank()
                ? nomeUsuario.split("\\s+")[0]
                : "cidadão";

        return switch (veredito) {
            case GOLPE -> new ExplicacaoAcessivel(
                    List.of(
                            "Isso tem fortes sinais de golpe.",
                            "Órgãos oficiais e bancos não pedem selfie, senha ou Pix por mensagem.",
                            "Não responda, não clique e, na dúvida, ligue 135."
                    ),
                    "Atenção. Isso tem fortes sinais de golpe. Não clique em links, não envie fotos e não faça pagamentos. Na dúvida, ligue 135.",
                    "Alerta Confere: " + primeiroNome + " recebeu uma mensagem com alto risco de golpe (score " + analise.score() + ").",
                    "Bloqueie o remetente e denuncie no Radar do Confere."
            );
            case SUSPEITO -> new ExplicacaoAcessivel(
                    List.of(
                            "Esta mensagem tem sinais suspeitos.",
                            "Não envie documentos, senhas ou pagamentos antes de confirmar.",
                            "Na dúvida, ligue 135 ou procure o canal oficial."
                    ),
                    "Cuidado. Esta mensagem tem sinais suspeitos. Antes de fazer qualquer coisa, confirme com um familiar ou pelo canal oficial 135.",
                    "Aviso Confere: " + primeiroNome + " verificou uma mensagem suspeita (score " + analise.score() + ").",
                    "Converse com seu guardião ou ligue para o órgão oficial antes de agir."
            );
            case DIREITO -> new ExplicacaoAcessivel(
                    List.of(
                            "Esta mensagem fala sobre um possível benefício ou direito seu.",
                            "Você pode conferir se tem direito sem pagar nada por isso.",
                            "Na dúvida, consulte o aplicativo oficial ou o CRAS da sua cidade."
                    ),
                    "Boa notícia. Identificamos um possível direito ou benefício para você. Abra a aba Meus Direitos para ver o passo a passo oficial.",
                    primeiroNome + " conferiu um documento sobre possível benefício social.",
                    "Abra Meus Direitos no Confere para verificar sua elegibilidade."
            );
            case OFICIAL -> new ExplicacaoAcessivel(
                    List.of(
                            "Esta mensagem parece ser de um canal oficial.",
                            "Mesmo assim, nunca compartilhe senhas ou códigos por mensagem.",
                            "Na dúvida, confirme diretamente no gov.br ou ligue 135."
                    ),
                    "Esta mensagem parece ser oficial. Mesmo assim, confira os prazos diretamente no aplicativo do órgão ou ligue 135.",
                    primeiroNome + " conferiu uma mensagem considerada oficial.",
                    "Acesse o canal oficial gov.br para seguir as orientações."
            );
            case INCONCLUSIVO -> new ExplicacaoAcessivel(
                    List.of(
                            "Não foi possível ler ou confirmar esta mensagem com clareza.",
                            "Tente enviar uma foto mais nítida ou colar o texto completo.",
                            "Na dúvida, não faça nada e ligue 135."
                    ),
                    "Não conseguimos ler a mensagem com clareza. Envie uma foto mais nítida ou peça ajuda a um familiar antes de agir.",
                    primeiroNome + " enviou uma mensagem que ficou inconclusiva.",
                    "Tire uma nova foto com boa iluminação ou cole o texto completo."
            );
        };
    }
}
