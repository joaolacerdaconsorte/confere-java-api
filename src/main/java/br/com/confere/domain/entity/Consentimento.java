package br.com.confere.domain.entity;

import br.com.confere.domain.enums.FinalidadeConsentimento;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CF_CONSENTIMENTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consentimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CONSENTIMENTO")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "FINALIDADE", nullable = false, length = 20)
    private FinalidadeConsentimento finalidade;

    @Column(name = "VERSAO_TERMO", nullable = false, length = 10)
    private String versaoTermo;

    @Convert(converter = SimNaoConverter.class)
    @Column(name = "ACEITO", nullable = false, columnDefinition = "CHAR(1)")
    private Boolean aceito;

    @Column(name = "DT_REGISTRO", nullable = false)
    private LocalDateTime dtRegistro;

    @PrePersist
    void prePersist() {
        if (dtRegistro == null) {
            dtRegistro = LocalDateTime.now();
        }
    }
}
