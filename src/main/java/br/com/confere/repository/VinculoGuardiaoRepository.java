package br.com.confere.repository;

import br.com.confere.domain.entity.VinculoGuardiao;
import br.com.confere.domain.enums.StatusVinculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VinculoGuardiaoRepository extends JpaRepository<VinculoGuardiao, Long> {

    @Query("SELECT v FROM VinculoGuardiao v JOIN FETCH v.guardiao g JOIN FETCH v.protegido p WHERE p.id = :protegidoId ORDER BY v.dtCriacao DESC")
    List<VinculoGuardiao> findByProtegidoIdOrderByDtCriacaoDesc(Long protegidoId);

    @Query("SELECT v FROM VinculoGuardiao v JOIN FETCH v.protegido p JOIN FETCH v.guardiao g WHERE g.id = :guardiaoId AND v.status = :status ORDER BY v.dtCriacao DESC")
    List<VinculoGuardiao> findByGuardiaoIdAndStatusOrderByDtCriacaoDesc(Long guardiaoId, StatusVinculo status);

    List<VinculoGuardiao> findByProtegidoIdAndStatus(Long protegidoId, StatusVinculo status);

    Optional<VinculoGuardiao> findByProtegidoIdAndGuardiaoId(Long protegidoId, Long guardiaoId);

    long countByProtegidoIdAndStatus(Long protegidoId, StatusVinculo status);
}
