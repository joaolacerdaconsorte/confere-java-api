package br.com.confere.service;

import br.com.confere.domain.entity.Consentimento;
import br.com.confere.domain.entity.Usuario;
import br.com.confere.domain.enums.FinalidadeConsentimento;
import br.com.confere.dto.ConsentimentoRequest;
import br.com.confere.dto.ConsentimentoResponse;
import br.com.confere.exception.ConfereException;
import br.com.confere.repository.ConsentimentoRepository;
import br.com.confere.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsentimentoService {

    private final ConsentimentoRepository consentimentoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<ConsentimentoResponse> listar(Long usuarioId) {
        return consentimentoRepository.findByUsuarioIdOrderByDtRegistroDesc(usuarioId).stream()
                .map(c -> new ConsentimentoResponse(
                        c.getId(),
                        c.getFinalidade(),
                        c.getVersaoTermo(),
                        Boolean.TRUE.equals(c.getAceito()),
                        TimeUtils.toOffset(c.getDtRegistro())
                ))
                .toList();
    }

    @Transactional
    public ConsentimentoResponse registrar(Long usuarioId, ConsentimentoRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ConfereException(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Usuário não encontrado."));

        Consentimento consentimento = Consentimento.builder()
                .usuario(usuario)
                .finalidade(request.finalidade())
                .versaoTermo(request.versaoTermo().trim())
                .aceito(request.aceito())
                .build();

        Consentimento salvo = consentimentoRepository.save(consentimento);
        return new ConsentimentoResponse(
                salvo.getId(),
                salvo.getFinalidade(),
                salvo.getVersaoTermo(),
                Boolean.TRUE.equals(salvo.getAceito()),
                TimeUtils.toOffset(salvo.getDtRegistro())
        );
    }

    /**
     * RN02: vale o registro mais recente por finalidade.
     */
    @Transactional(readOnly = true)
    public boolean temConsentimentoAtivo(Long usuarioId, FinalidadeConsentimento finalidade) {
        return consentimentoRepository
                .findTopByUsuarioIdAndFinalidadeOrderByDtRegistroDesc(usuarioId, finalidade)
                .map(c -> Boolean.TRUE.equals(c.getAceito()))
                .orElse(false);
    }
}
