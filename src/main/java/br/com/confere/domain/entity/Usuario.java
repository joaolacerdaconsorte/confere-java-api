package br.com.confere.domain.entity;

import br.com.confere.domain.enums.PerfilUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CF_USUARIO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO")
    private Long id;

    @Column(name = "NOME", nullable = false, length = 120)
    private String nome;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "TELEFONE", length = 20)
    private String telefone;

    @Column(name = "SENHA_HASH", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "PERFIL", nullable = false, length = 10)
    @Builder.Default
    private PerfilUsuario perfil = PerfilUsuario.CIDADAO;

    @Column(name = "DT_NASCIMENTO")
    private LocalDate dtNascimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_MUNICIPIO", nullable = false)
    private Municipio municipio;

    @Convert(converter = SimNaoConverter.class)
    @Column(name = "FONTE_GRANDE", nullable = false, columnDefinition = "CHAR(1)")
    @Builder.Default
    private Boolean fonteGrande = false;

    @Convert(converter = SimNaoConverter.class)
    @Column(name = "AUDIO_ATIVO", nullable = false, columnDefinition = "CHAR(1)")
    @Builder.Default
    private Boolean audioAtivo = true;

    @Column(name = "EXPO_PUSH_TOKEN", length = 200)
    private String expoPushToken;

    @Convert(converter = SimNaoConverter.class)
    @Column(name = "ATIVO", nullable = false, columnDefinition = "CHAR(1)")
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "DT_CADASTRO", nullable = false)
    private LocalDateTime dtCadastro;

    @PrePersist
    void prePersist() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (dtCadastro == null) {
            dtCadastro = LocalDateTime.now();
        }
        if (perfil == null) {
            perfil = PerfilUsuario.CIDADAO;
        }
        if (fonteGrande == null) {
            fonteGrande = false;
        }
        if (audioAtivo == null) {
            audioAtivo = true;
        }
        if (ativo == null) {
            ativo = true;
        }
    }
}
