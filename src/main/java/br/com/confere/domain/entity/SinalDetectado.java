package br.com.confere.domain.entity;

import br.com.confere.domain.enums.TipoSinal;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "CF_SINAL_DETECTADO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinalDetectado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_SINAL")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_VERIFICACAO", nullable = false)
    private Verificacao verificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_SINAL", nullable = false, length = 25)
    private TipoSinal tipoSinal;

    @Column(name = "TRECHO", length = 300)
    private String trecho;

    @Column(name = "PESO", precision = 3, scale = 2)
    private BigDecimal peso;
}
