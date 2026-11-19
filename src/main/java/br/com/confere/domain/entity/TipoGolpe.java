package br.com.confere.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CF_TIPO_GOLPE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoGolpe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_TIPO_GOLPE")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "NOME", nullable = false, length = 80)
    private String nome;

    @Column(name = "DESCRICAO", length = 500)
    private String descricao;

    @Column(name = "GRAVIDADE", nullable = false)
    private Integer gravidade;

    @Column(name = "ORGAO_IMITADO", length = 60)
    private String orgaoImitado;
}
