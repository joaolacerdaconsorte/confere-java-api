package br.com.confere.domain.entity;

import br.com.confere.domain.enums.DesfechoFeedback;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CF_FEEDBACK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_FEEDBACK")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_VERIFICACAO", nullable = false, unique = true)
    private Verificacao verificacao;

    @Convert(converter = SimNaoConverter.class)
    @Column(name = "FOI_UTIL", nullable = false, columnDefinition = "CHAR(1)")
    private Boolean foiUtil;

    @Enumerated(EnumType.STRING)
    @Column(name = "DESFECHO", length = 15)
    private DesfechoFeedback desfecho;

    @Column(name = "VALOR_EVITADO", precision = 12, scale = 2)
    private BigDecimal valorEvitado;

    @Column(name = "DT_REGISTRO", nullable = false)
    private LocalDateTime dtRegistro;

    @PrePersist
    void prePersist() {
        if (dtRegistro == null) {
            dtRegistro = LocalDateTime.now();
        }
    }
}
