package br.com.confere.repository;

import br.com.confere.domain.entity.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MunicipioRepository extends JpaRepository<Municipio, Long> {
    Optional<Municipio> findByCodIbge(Integer codIbge);
}
