package br.com.confere.repository;

import br.com.confere.domain.entity.Consentimento;
import br.com.confere.domain.enums.FinalidadeConsentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, Long> {
    List<Consentimento> findByUsuarioIdOrderByDtRegistroDesc(Long usuarioId);
    Optional<Consentimento> findTopByUsuarioIdAndFinalidadeOrderByDtRegistroDesc(Long usuarioId, FinalidadeConsentimento finalidade);
}
