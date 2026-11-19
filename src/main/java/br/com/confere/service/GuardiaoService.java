package br.com.confere.service;

import br.com.confere.domain.entity.AlertaGuardiao;
import br.com.confere.domain.entity.Usuario;
import br.com.confere.domain.entity.VinculoGuardiao;
import br.com.confere.domain.enums.NivelMinimoAlerta;
import br.com.confere.domain.enums.StatusAlerta;
import br.com.confere.domain.enums.StatusVinculo;
import br.com.confere.dto.*;
import br.com.confere.exception.ConfereException;
import br.com.confere.repository.AlertaGuardiaoRepository;
import br.com.confere.repository.UsuarioRepository;
import br.com.confere.repository.VinculoGuardiaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardiaoService {

    private static final int LIMITE_GUARDIOES_ATIVOS = 5;

    private final VinculoGuardiaoRepository vinculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlertaGuardiaoRepository alertaRepository;

    @Transactional(readOnly = true)
    public List<VinculoGuardiaoResponse> listarMeusGuardioes(Long protegidoId) {
        return vinculoRepository.findByProtegidoIdOrderByDtCriacaoDesc(protegidoId).stream()
                .map(this::toVinculoResponse)
                .toList();
    }

    /**
     * RN10: convite só para e-mail cadastrado; proibido auto-convite e duplicado (409); limite 5 ativos.
     */
    @Transactional
    public VinculoGuardiaoResponse convidar(Long protegidoId, ConvidarGuardiaoRequest request) {
        Usuario protegido = usuarioRepository.findById(protegidoId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Usuário não encontrado."));

        String emailGuardiao = request.emailGuardiao().trim().toLowerCase();
        Usuario guardiao = usuarioRepository.findByEmailIgnoreCase(emailGuardiao)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO",
                        "Nenhum usuário cadastrado encontrado com o e-mail informado."));

        if (protegido.getId().equals(guardiao.getId())) {
            throw new ConfereException(HttpStatus.BAD_REQUEST, "VALIDACAO",
                    "Não é possível convidar a si mesmo como guardião.");
        }

        if (vinculoRepository.countByProtegidoIdAndStatus(protegidoId, StatusVinculo.ATIVO) >= LIMITE_GUARDIOES_ATIVOS) {
            throw new ConfereException(HttpStatus.CONFLICT, "CONFLITO",
                    "Limite máximo de " + LIMITE_GUARDIOES_ATIVOS + " guardiões ativos atingido.");
        }

        var existente = vinculoRepository.findByProtegidoIdAndGuardiaoId(protegidoId, guardiao.getId());
        if (existente.isPresent()) {
            VinculoGuardiao v = existente.get();
            if (v.getStatus() == StatusVinculo.PENDENTE || v.getStatus() == StatusVinculo.ATIVO) {
                throw new ConfereException(HttpStatus.CONFLICT, "GUARDIAO_DUPLICADO",
                        "Este guardião já possui um vínculo ativo ou pendente.");
            }
            // Reativa convite anteriormente revogado/recusado
            v.setParentesco(request.parentesco().trim());
            v.setNivelMinimo(request.nivelMinimo() != null ? request.nivelMinimo() : NivelMinimoAlerta.GOLPE);
            v.setStatus(StatusVinculo.PENDENTE);
            v.setDtCriacao(LocalDateTime.now());
            v.setDtAceite(null);
            return toVinculoResponse(vinculoRepository.save(v));
        }

        VinculoGuardiao novo = VinculoGuardiao.builder()
                .protegido(protegido)
                .guardiao(guardiao)
                .parentesco(request.parentesco().trim())
                .nivelMinimo(request.nivelMinimo() != null ? request.nivelMinimo() : NivelMinimoAlerta.GOLPE)
                .status(StatusVinculo.PENDENTE)
                .build();

        return toVinculoResponse(vinculoRepository.save(novo));
    }

    @Transactional
    public VinculoGuardiaoResponse atualizar(Long protegidoId, Long idVinculo, AtualizarGuardiaoRequest request) {
        VinculoGuardiao vinculo = vinculoRepository.findById(idVinculo)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Vínculo não encontrado."));

        if (!vinculo.getProtegido().getId().equals(protegidoId)) {
            throw new ConfereException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Você não pode alterar este vínculo.");
        }

        vinculo.setParentesco(request.parentesco().trim());
        vinculo.setNivelMinimo(request.nivelMinimo());
        return toVinculoResponse(vinculoRepository.save(vinculo));
    }

    @Transactional
    public void revogar(Long protegidoId, Long idVinculo) {
        VinculoGuardiao vinculo = vinculoRepository.findById(idVinculo)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Vínculo não encontrado."));

        if (!vinculo.getProtegido().getId().equals(protegidoId)) {
            throw new ConfereException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Você não pode revogar este vínculo.");
        }

        vinculo.setStatus(StatusVinculo.REVOGADO);
        vinculoRepository.save(vinculo);
    }

    @Transactional(readOnly = true)
    public List<VinculoGuardiaoResponse> listarConvitesRecebidos(Long guardiaoId) {
        return vinculoRepository.findByGuardiaoIdAndStatusOrderByDtCriacaoDesc(guardiaoId, StatusVinculo.PENDENTE)
                .stream()
                .map(this::toVinculoResponse)
                .toList();
    }

    /**
     * RN10: somente o convidado pode aceitar ou recusar.
     */
    @Transactional
    public VinculoGuardiaoResponse responderConvite(Long guardiaoId, Long idVinculo, boolean aceitar) {
        VinculoGuardiao vinculo = vinculoRepository.findById(idVinculo)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Convite não encontrado."));

        if (!vinculo.getGuardiao().getId().equals(guardiaoId)) {
            throw new ConfereException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Somente o guardião convidado pode responder.");
        }

        if (vinculo.getStatus() != StatusVinculo.PENDENTE) {
            throw new ConfereException(HttpStatus.CONFLICT, "CONFLITO", "Este convite não está mais pendente.");
        }

        if (aceitar) {
            long ativos = vinculoRepository.countByProtegidoIdAndStatus(vinculo.getProtegido().getId(), StatusVinculo.ATIVO);
            if (ativos >= LIMITE_GUARDIOES_ATIVOS) {
                throw new ConfereException(HttpStatus.CONFLICT, "CONFLITO",
                        "O protegido já atingiu o limite de 5 guardiões ativos.");
            }
            vinculo.setStatus(StatusVinculo.ATIVO);
            vinculo.setDtAceite(LocalDateTime.now());
        } else {
            vinculo.setStatus(StatusVinculo.RECUSADO);
        }

        return toVinculoResponse(vinculoRepository.save(vinculo));
    }

    @Transactional(readOnly = true)
    public List<VinculoGuardiaoResponse> listarProtegidos(Long guardiaoId) {
        return vinculoRepository.findByGuardiaoIdAndStatusOrderByDtCriacaoDesc(guardiaoId, StatusVinculo.ATIVO)
                .stream()
                .map(this::toVinculoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AlertaGuardiaoResponse> listarAlertas(Long guardiaoId, StatusAlerta status, int pagina, int tamanho) {
        int tam = Math.min(Math.max(tamanho, 1), 50);
        int pag = Math.max(pagina, 0);
        Page<AlertaGuardiao> page = alertaRepository.findAlertasDoGuardiao(guardiaoId, status, PageRequest.of(pag, tam));
        return PaginaResponse.of(page.map(this::toAlertaResponse));
    }

    @Transactional
    public AlertaGuardiaoResponse atualizarStatusAlerta(Long guardiaoId, Long idAlerta, AtualizarAlertaRequest request) {
        AlertaGuardiao alerta = alertaRepository.findByIdAndGuardiaoId(idAlerta, guardiaoId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Alerta não encontrado."));

        if (alerta.getVinculo().getStatus() != StatusVinculo.ATIVO) {
            throw new ConfereException(HttpStatus.FORBIDDEN, "SEM_PERMISSAO", "Vínculo de guardião não está ativo.");
        }

        alerta.setStatus(request.status());
        if (request.status() == StatusAlerta.LIDO && alerta.getDtLeitura() == null) {
            alerta.setDtLeitura(LocalDateTime.now());
        }
        return toAlertaResponse(alertaRepository.save(alerta));
    }

    private VinculoGuardiaoResponse toVinculoResponse(VinculoGuardiao v) {
        return new VinculoGuardiaoResponse(
                v.getId(),
                new PessoaResumoDto(v.getGuardiao().getId(), v.getGuardiao().getNome(), v.getGuardiao().getEmail()),
                new PessoaResumoDto(v.getProtegido().getId(), v.getProtegido().getNome(), v.getProtegido().getEmail()),
                v.getParentesco(),
                v.getNivelMinimo(),
                v.getStatus(),
                TimeUtils.toOffset(v.getDtCriacao()),
                TimeUtils.toOffset(v.getDtAceite())
        );
    }

    private AlertaGuardiaoResponse toAlertaResponse(AlertaGuardiao a) {
        return new AlertaGuardiaoResponse(
                a.getId(),
                a.getVerificacao().getId(),
                a.getVinculo().getProtegido().getNome(),
                a.getVerificacao().getVeredito(),
                a.getVerificacao().getScoreRisco(),
                a.getVerificacao().getMensagemGuardiao(),
                a.getCanalEnvio(),
                a.getStatus(),
                TimeUtils.toOffset(a.getDtEnvio()),
                TimeUtils.toOffset(a.getDtLeitura())
        );
    }
}
