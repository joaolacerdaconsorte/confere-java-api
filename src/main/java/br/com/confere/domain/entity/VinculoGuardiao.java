package br.com.confere.domain.entity;

import br.com.confere.domain.enums.NivelMinimoAlerta;
import br.com.confere.domain.enums.StatusVinculo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "CF_VINCULO_GUARDIAO",
    uniqueConstraints = @UniqueConstraint(name = "UQ_VINCULO_PAR", columnNames = {"ID_PROTEGIDO", "ID_GUARDIAO"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VinculoGuardiao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_VINCULO")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_PROTEGIDO", nullable = false)
    private Usuario protegido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_GUARDIAO", nullable = false)
    private Usuario guardiao;

    @Column(name = "PARENTESCO", nullable = false, length = 30)
    private String parentesco;

    @Enumerated(EnumType.STRING)
    @Column(name = "NIVEL_MINIMO", nullable = false, length = 10)
    @Builder.Default
    private NivelMinimoAlerta nivelMinimo = NivelMinimoAlerta.GOLPE;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    @Builder.Default
    private StatusVinculo status = StatusVinculo.PENDENTE;

    @Column(name = "DT_CRIACAO", nullable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "DT_ACEITE")
    private LocalDateTime dtAceite;

    @PrePersist
    void prePersist() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now();
        }
        if (nivelMinimo == null) {
            nivelMinimo = NivelMinimoAlerta.GOLPE;
        }
        if (status == null) {
            status = StatusVinculo.PENDENTE;
        }
    }
}
