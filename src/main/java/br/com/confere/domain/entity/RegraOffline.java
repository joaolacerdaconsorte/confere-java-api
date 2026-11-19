package br.com.confere.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "CF_REGRA_OFFLINE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegraOffline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_REGRA")
    private Long id;

    @Column(name = "PADRAO", nullable = false, length = 300)
    private String padrao;

    @Column(name = "TIPO_SINAL", nullable = false, length = 25)
    private String tipoSinal;

    @Column(name = "PESO", nullable = false, precision = 3, scale = 2)
    private BigDecimal peso;

    @Column(name = "MENSAGEM", nullable = false, length = 300)
    private String mensagem;

    @Column(name = "VERSAO", nullable = false)
    private Integer versao;

    @Convert(converter = SimNaoConverter.class)
    @Column(name = "ATIVO", nullable = false, columnDefinition = "CHAR(1)")
    @Builder.Default
    private Boolean ativo = true;

    @PrePersist
    void prePersist() {
        if (ativo == null) {
            ativo = true;
        }
    }
}
