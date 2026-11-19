package br.com.confere.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CF_CANAL_ORIGEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanalOrigem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CANAL")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "NOME", nullable = false, length = 40)
    private String nome;
}
