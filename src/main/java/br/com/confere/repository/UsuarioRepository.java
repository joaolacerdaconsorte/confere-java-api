package br.com.confere.repository;

import br.com.confere.domain.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("SELECT u FROM Usuario u JOIN FETCH u.municipio WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Usuario> findByEmailIgnoreCaseWithMunicipio(String email);

    @Query("SELECT u FROM Usuario u JOIN FETCH u.municipio WHERE u.id = :id")
    Optional<Usuario> findByIdWithMunicipio(Long id);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
