package br.com.confere.repository;

import br.com.confere.domain.entity.RegraOffline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegraOfflineRepository extends JpaRepository<RegraOffline, Long> {
    List<RegraOffline> findByAtivoTrueOrderByIdAsc();

    @Query("SELECT COALESCE(MAX(r.versao), 0) FROM RegraOffline r WHERE r.ativo = true")
    Integer findMaxVersaoAtiva();
}
