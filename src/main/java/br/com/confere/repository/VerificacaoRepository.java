package br.com.confere.repository;

import br.com.confere.domain.entity.Verificacao;
import br.com.confere.domain.enums.Veredito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacaoRepository extends JpaRepository<Verificacao, Long> {
    Page<Verificacao> findByUsuarioIdOrderByDtCriacaoDesc(Long usuarioId, Pageable pageable);
    Page<Verificacao> findByUsuarioIdAndVereditoOrderByDtCriacaoDesc(Long usuarioId, Veredito veredito, Pageable pageable);
    Optional<Verificacao> findByIdAndUsuarioId(Long id, Long usuarioId);
}
