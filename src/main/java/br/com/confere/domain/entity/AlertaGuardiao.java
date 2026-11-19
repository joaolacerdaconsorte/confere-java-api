package br.com.confere.domain.entity;

import br.com.confere.domain.enums.CanalEnvioAlerta;
import br.com.confere.domain.enums.StatusAlerta;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "CF_ALERTA_GUARDIAO",
    uniqueConstraints = @UniqueConstraint(name = "UQ_ALERTA_VERIF_VINC", columnNames = {"ID_VERIFICACAO", "ID_VINCULO"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaGuardiao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ALERTA")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_VERIFICACAO", nullable = false)
    private Verificacao verificacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_VINCULO", nullable = false)
    private VinculoGuardiao vinculo;

    @Enumerated(EnumType.STRING)
    @Column(name = "CANAL_ENVIO", nullable = false, length = 10)
    @Builder.Default
    private CanalEnvioAlerta canalEnvio = CanalEnvioAlerta.PUSH;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    @Builder.Default
    private StatusAlerta status = StatusAlerta.ENVIADO;

    @Column(name = "DT_ENVIO", nullable = false)
    private LocalDateTime dtEnvio;

    @Column(name = "DT_LEITURA")
    private LocalDateTime dtLeitura;

    @PrePersist
    void prePersist() {
        if (dtEnvio == null) {
            dtEnvio = LocalDateTime.now();
        }
        if (canalEnvio == null) {
            canalEnvio = CanalEnvioAlerta.PUSH;
        }
        if (status == null) {
            status = StatusAlerta.ENVIADO;
        }
    }
}
