package br.com.confere.service;

import br.com.confere.ai.ExplicacaoAcessivel;
import br.com.confere.ai.ExplicadorAcessivelService;
import br.com.confere.client.AnaliseIaClient;
import br.com.confere.client.AnaliseIaResponse;
import br.com.confere.domain.entity.*;
import br.com.confere.domain.enums.*;
import br.com.confere.dto.*;
import br.com.confere.exception.ConfereException;
import br.com.confere.messaging.VerificacaoOfflineEvent;
import br.com.confere.messaging.VerificacaoRiscoEvent;
import br.com.confere.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificacaoService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final VerificacaoRepository verificacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CanalOrigemRepository canalOrigemRepository;
    private final TipoGolpeRepository tipoGolpeRepository;
    private final VinculoGuardiaoRepository vinculoRepository;
    private final FeedbackRepository feedbackRepository;
    private final ConsentimentoService consentimentoService;
    private final MascaramentoService mascaramentoService;
    private final AnaliseIaClient analiseIaClient;
    private final ExplicadorAcessivelService explicadorService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public VerificacaoResponse criar(Long usuarioId, CriarVerificacaoRequest request, MultipartFile imagem) {
        // RN02: exige consentimento ANALISE_IA aceito
        if (!consentimentoService.temConsentimentoAtivo(usuarioId, FinalidadeConsentimento.ANALISE_IA)) {
            throw new ConfereException(HttpStatus.FORBIDDEN, "CONSENTIMENTO_PENDENTE",
                    "É necessário aceitar o termo de consentimento de análise por IA antes de verificar.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Usuário não encontrado."));

        CanalOrigem canal = null;
        if (request.canal() != null && !request.canal().isBlank()) {
            canal = canalOrigemRepository.findByCodigoIgnoreCase(request.canal().trim())
                    .orElseThrow(() -> new ConfereException(HttpStatus.BAD_REQUEST, "VALIDACAO",
                            "Canal de origem inválido: " + request.canal()));
        }

        String imagemBase64 = null;
        byte[] imagemBytes = null;
        if (imagem != null && !imagem.isEmpty()) {
            validarImagem(imagem);
            try {
                imagemBytes = imagem.getBytes();
                imagemBase64 = Base64.getEncoder().encodeToString(imagemBytes);
            } catch (IOException e) {
                throw new ConfereException(HttpStatus.BAD_REQUEST, "VALIDACAO", "Erro ao ler o arquivo de imagem.");
            }
        }

        String conteudoOriginal = request.conteudo() != null ? request.conteudo() : "";
        if (conteudoOriginal.isBlank() && imagemBase64 == null) {
            throw new ConfereException(HttpStatus.BAD_REQUEST, "VALIDACAO",
                    "Informe o texto/link ou envie uma imagem para verificação.");
        }

        // RN01: mascarar antes de tudo; hash SHA-256 do conteúdo original
        String conteudoMascarado = limitar(mascaramentoService.mascarar(conteudoOriginal), 4000);
        String hashConteudo = imagemBytes != null
                ? mascaramentoService.sha256(imagemBytes)
                : mascaramentoService.sha256(conteudoOriginal);

        ModoVerificacao modo = request.modo() != null ? request.modo() : ModoVerificacao.ONLINE;

        if (modo == ModoVerificacao.OFFLINE) {
            // RN08: fila confere.verificacao.offline e resposta 202
            Verificacao pendente = Verificacao.builder()
                    .usuario(usuario)
                    .canal(canal)
                    .tipoEntrada(request.tipoEntrada())
                    .conteudoMascarado(conteudoMascarado)
                    .hashConteudo(hashConteudo)
                    .modo(ModoVerificacao.OFFLINE)
                    .status(StatusVerificacao.PENDENTE)
                    .build();

            Verificacao salva = verificacaoRepository.save(pendente);
            eventPublisher.publishEvent(new VerificacaoOfflineEvent(
                    salva.getId(),
                    usuario.getId(),
                    conteudoMascarado,
                    canal != null ? canal.getCodigo() : null
            ));
            return toResponse(salva, List.of(), 0, null);
        }

        return executarAnaliseECompletar(usuario, canal, request.tipoEntrada(), conteudoMascarado, hashConteudo,
                imagemBase64, ModoVerificacao.ONLINE, null);
    }

    @Transactional
    public void processarOffline(Long verificacaoId) {
        Verificacao verificacao = verificacaoRepository.findById(verificacaoId).orElse(null);
        if (verificacao == null || verificacao.getStatus() == StatusVerificacao.PROCESSADA) {
            return;
        }
        executarAnaliseECompletar(
                verificacao.getUsuario(),
                verificacao.getCanal(),
                verificacao.getTipoEntrada(),
                verificacao.getConteudoMascarado(),
                verificacao.getHashConteudo(),
                null,
                ModoVerificacao.OFFLINE,
                verificacao
        );
    }

    private VerificacaoResponse executarAnaliseECompletar(
            Usuario usuario,
            CanalOrigem canal,
            TipoEntrada tipoEntrada,
            String conteudoMascarado,
            String hashConteudo,
            String imagemBase64,
            ModoVerificacao modo,
            Verificacao existente
    ) {
        String canalCodigo = canal != null ? canal.getCodigo() : "WHATSAPP";
        AnaliseIaResponse analise = analiseIaClient.analisar(conteudoMascarado, imagemBase64, canalCodigo);

        String textoParaAvaliacao = analise.textoMascarado() != null && !analise.textoMascarado().isBlank()
                ? mascaramentoService.mascarar(analise.textoMascarado())
                : conteudoMascarado;

        // RN04: regra de definição do veredito
        Veredito veredito = resolverVeredito(analise, textoParaAvaliacao);
        int score = Math.max(0, Math.min(100, analise.score()));

        TipoGolpe tipoGolpe = null;
        if (analise.tipoGolpeCodigo() != null && !analise.tipoGolpeCodigo().isBlank()
                && (veredito == Veredito.GOLPE || veredito == Veredito.SUSPEITO)) {
            tipoGolpe = tipoGolpeRepository.findByCodigoIgnoreCase(analise.tipoGolpeCodigo()).orElse(null);
        }

        // Spring AI: explicação acessível obedecendo RN05
        ExplicacaoAcessivel explicacao = explicadorService.gerarExplicacao(veredito, analise, usuario.getNome());

        Verificacao verificacao = existente != null ? existente : Verificacao.builder().build();
        verificacao.setUsuario(usuario);
        verificacao.setCanal(canal);
        verificacao.setTipoGolpe(tipoGolpe);
        verificacao.setTipoEntrada(tipoEntrada);
        verificacao.setConteudoMascarado(limitar(textoParaAvaliacao, 4000));
        verificacao.setHashConteudo(hashConteudo);
        verificacao.setVeredito(veredito);
        verificacao.setScoreRisco(score);
        verificacao.setExplicacao(serializarFrases(explicacao.frases()));
        verificacao.setRoteiroAudio(limitar(explicacao.roteiroAudio(), 1000));
        verificacao.setMensagemGuardiao(limitar(explicacao.mensagemGuardiao(), 500));
        verificacao.setModo(modo);
        verificacao.setStatus(StatusVerificacao.PROCESSADA);
        verificacao.setDtProcessamento(LocalDateTime.now());

        verificacao.getSinais().clear();
        if (analise.sinais() != null) {
            for (AnaliseIaResponse.SinalIa s : analise.sinais()) {
                TipoSinal tipoSinal = parseTipoSinal(s.tipo());
                if (tipoSinal != null) {
                    verificacao.getSinais().add(SinalDetectado.builder()
                            .verificacao(verificacao)
                            .tipoSinal(tipoSinal)
                            .trecho(limitar(s.trecho(), 300))
                            .peso(s.peso())
                            .build());
                }
            }
        }

        Verificacao salva = verificacaoRepository.save(verificacao);

        int guardioesAvisados = 0;
        if (veredito == Veredito.GOLPE || veredito == Veredito.SUSPEITO) {
            guardioesAvisados = (int) vinculoRepository
                    .findByProtegidoIdAndStatus(usuario.getId(), StatusVinculo.ATIVO)
                    .stream()
                    .filter(v -> veredito == Veredito.GOLPE || v.getNivelMinimo() == NivelMinimoAlerta.SUSPEITO)
                    .count();

            // RN06: publica evento após commit
            eventPublisher.publishEvent(new VerificacaoRiscoEvent(
                    salva.getId(),
                    usuario.getId(),
                    veredito,
                    score,
                    explicacao.mensagemGuardiao()
            ));
        }

        List<FonteDto> fontes = analise.fontes() == null ? List.of() :
                analise.fontes().stream().map(f -> new FonteDto(f.titulo(), f.url())).toList();

        return toResponse(salva, fontes, guardioesAvisados, explicacao.proximoPasso());
    }

    /**
     * RN04: INCONCLUSIVO se confiança OCR < 0.5 ou texto < 15 caracteres.
     * Senão: GOLPE se score >= 70; SUSPEITO se 40 <= score <= 69; abaixo de 40, OFICIAL ou DIREITO.
     */
    public Veredito resolverVeredito(AnaliseIaResponse analise, String texto) {
        if (analise.ocrConfianca() != null && analise.ocrConfianca() < 0.5) {
            return Veredito.INCONCLUSIVO;
        }
        if (texto == null || texto.trim().length() < 15) {
            return Veredito.INCONCLUSIVO;
        }
        int score = analise.score();
        if (score >= 70) {
            return Veredito.GOLPE;
        }
        if (score >= 40) {
            return Veredito.SUSPEITO;
        }
        if ("DIREITO".equalsIgnoreCase(analise.classe()) || "DIREITO".equalsIgnoreCase(analise.veredito())) {
            return Veredito.DIREITO;
        }
        return Veredito.OFICIAL;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VerificacaoResponse> listar(Long usuarioId, Veredito veredito, int pagina, int tamanho) {
        int tam = Math.min(Math.max(tamanho, 1), 50);
        int pag = Math.max(pagina, 0);
        Pageable pageable = PageRequest.of(pag, tam);

        // RN09: cidadão só vê as próprias verificações
        Page<Verificacao> page = veredito == null
                ? verificacaoRepository.findByUsuarioIdOrderByDtCriacaoDesc(usuarioId, pageable)
                : verificacaoRepository.findByUsuarioIdAndVereditoOrderByDtCriacaoDesc(usuarioId, veredito, pageable);

        return PaginaResponse.of(page.map(v -> toResponse(v, List.of(), v.getAlertas().size(), null)));
    }

    @Transactional(readOnly = true)
    public VerificacaoResponse detalhar(Long usuarioId, Long id) {
        Verificacao verificacao = verificacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Verificação não encontrada."));
        return toResponse(verificacao, List.of(), verificacao.getAlertas().size(), null);
    }

    /**
     * RN09: exclusão física em cascata de sinais, alertas e feedback.
     */
    @Transactional
    public void excluir(Long usuarioId, Long id) {
        Verificacao verificacao = verificacaoRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Verificação não encontrada."));
        verificacaoRepository.delete(verificacao);
    }

    @Transactional
    public FeedbackResponse salvarFeedback(Long usuarioId, Long verificacaoId, FeedbackRequest request) {
        Verificacao verificacao = verificacaoRepository.findByIdAndUsuarioId(verificacaoId, usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Verificação não encontrada."));

        Feedback feedback = feedbackRepository.findByVerificacaoId(verificacaoId)
                .orElseGet(() -> Feedback.builder().verificacao(verificacao).build());

        feedback.setFoiUtil(request.foiUtil());
        feedback.setDesfecho(request.desfecho());
        feedback.setValorEvitado(request.valorEvitado());
        feedback.setDtRegistro(LocalDateTime.now());

        Feedback salvo = feedbackRepository.save(feedback);
        return new FeedbackResponse(
                salvo.getId(),
                Boolean.TRUE.equals(salvo.getFoiUtil()),
                salvo.getDesfecho(),
                salvo.getValorEvitado(),
                TimeUtils.toOffset(salvo.getDtRegistro())
        );
    }

    private void validarImagem(MultipartFile imagem) {
        if (imagem.getSize() > MAX_IMAGE_BYTES) {
            throw new ConfereException(HttpStatus.BAD_REQUEST, "VALIDACAO", "A imagem deve ter no máximo 5 MB.");
        }
        String contentType = imagem.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ConfereException(HttpStatus.BAD_REQUEST, "VALIDACAO", "O arquivo enviado deve ser uma imagem.");
        }
    }

    private TipoSinal parseTipoSinal(String tipo) {
        if (tipo == null) {
            return null;
        }
        try {
            return TipoSinal.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String serializarFrases(List<String> frases) {
        try {
            return objectMapper.writeValueAsString(frases == null ? List.of() : frases);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> deserializarFrases(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private VerificacaoResponse toResponse(Verificacao v, List<FonteDto> fontes, int guardioesAvisados, String proximoPasso) {
        TipoGolpeResumoDto tipoGolpeDto = v.getTipoGolpe() != null
                ? new TipoGolpeResumoDto(v.getTipoGolpe().getCodigo(), v.getTipoGolpe().getNome())
                : null;

        List<SinalDto> sinais = v.getSinais() == null ? List.of() :
                v.getSinais().stream()
                        .map(s -> new SinalDto(s.getTipoSinal().name(), s.getTrecho()))
                        .toList();

        return new VerificacaoResponse(
                v.getId(),
                v.getVeredito(),
                v.getScoreRisco(),
                v.getStatus(),
                tipoGolpeDto,
                deserializarFrases(v.getExplicacao()),
                v.getRoteiroAudio(),
                proximoPasso,
                sinais,
                fontes,
                guardioesAvisados,
                TimeUtils.toOffset(v.getDtCriacao()),
                v.getModo(),
                v.getConteudoMascarado()
        );
    }

    private String limitar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }
}
