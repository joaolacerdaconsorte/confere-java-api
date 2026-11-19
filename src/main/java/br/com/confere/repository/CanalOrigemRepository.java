package br.com.confere.repository;

import br.com.confere.domain.entity.CanalOrigem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CanalOrigemRepository extends JpaRepository<CanalOrigem, Long> {
    Optional<CanalOrigem> findByCodigoIgnoreCase(String codigo);
}
