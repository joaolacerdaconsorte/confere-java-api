package br.com.confere.repository;

import br.com.confere.domain.entity.AlertaGuardiao;
import br.com.confere.domain.enums.StatusAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AlertaGuardiaoRepository extends JpaRepository<AlertaGuardiao, Long> {

    @Query("SELECT a FROM AlertaGuardiao a " +
           "JOIN FETCH a.verificacao v " +
           "JOIN FETCH a.vinculo vin " +
           "JOIN FETCH vin.protegido p " +
           "WHERE vin.guardiao.id = :guardiaoId AND vin.status = br.com.confere.domain.enums.StatusVinculo.ATIVO " +
           "AND (:status IS NULL OR a.status = :status) " +
           "ORDER BY a.dtEnvio DESC")
    Page<AlertaGuardiao> findAlertasDoGuardiao(Long guardiaoId, StatusAlerta status, Pageable pageable);

    @Query("SELECT a FROM AlertaGuardiao a JOIN FETCH a.vinculo vin WHERE a.id = :id AND vin.guardiao.id = :guardiaoId")
    Optional<AlertaGuardiao> findByIdAndGuardiaoId(Long id, Long guardiaoId);

    boolean existsByVerificacaoIdAndVinculoId(Long verificacaoId, Long vinculoId);
}
