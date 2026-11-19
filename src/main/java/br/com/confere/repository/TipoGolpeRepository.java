package br.com.confere.repository;

import br.com.confere.domain.entity.TipoGolpe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoGolpeRepository extends JpaRepository<TipoGolpe, Long> {
    Optional<TipoGolpe> findByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
}
