package br.com.confere.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CF_MUNICIPIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_MUNICIPIO")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @Column(name = "UF", nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String uf;

    @Column(name = "COD_IBGE", nullable = false, unique = true)
    private Integer codIbge;
}
