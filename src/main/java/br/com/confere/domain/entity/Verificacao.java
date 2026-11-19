package br.com.confere.domain.entity;

import br.com.confere.domain.enums.ModoVerificacao;
import br.com.confere.domain.enums.StatusVerificacao;
import br.com.confere.domain.enums.TipoEntrada;
import br.com.confere.domain.enums.Veredito;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "CF_VERIFICACAO",
    indexes = @Index(name = "IX_CF_VERIFICACAO_USR_DT", columnList = "ID_USUARIO, DT_CRIACAO")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_VERIFICACAO")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CANAL")
    private CanalOrigem canal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TIPO_GOLPE")
    private TipoGolpe tipoGolpe;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_ENTRADA", nullable = false, length = 10)
    private TipoEntrada tipoEntrada;

    @Column(name = "CONTEUDO_MASCARADO", length = 4000)
    private String conteudoMascarado;

    @Column(name = "HASH_CONTEUDO", nullable = false, length = 64)
    private String hashConteudo;

    @Enumerated(EnumType.STRING)
    @Column(name = "VEREDITO", length = 15)
    private Veredito veredito;

    @Column(name = "SCORE_RISCO")
    private Integer scoreRisco;

    @Column(name = "EXPLICACAO", length = 2000)
    private String explicacao;

    @Column(name = "ROTEIRO_AUDIO", length = 1000)
    private String roteiroAudio;

    @Column(name = "MENSAGEM_GUARDIAO", length = 500)
    private String mensagemGuardiao;

    @Enumerated(EnumType.STRING)
    @Column(name = "MODO", nullable = false, length = 8)
    @Builder.Default
    private ModoVerificacao modo = ModoVerificacao.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 12)
    @Builder.Default
    private StatusVerificacao status = StatusVerificacao.PENDENTE;

    @Column(name = "DT_CRIACAO", nullable = false)
    private LocalDateTime dtCriacao;

    @Column(name = "DT_PROCESSAMENTO")
    private LocalDateTime dtProcessamento;

    @OneToMany(mappedBy = "verificacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SinalDetectado> sinais = new ArrayList<>();

    @OneToMany(mappedBy = "verificacao", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AlertaGuardiao> alertas = new ArrayList<>();

    @OneToOne(mappedBy = "verificacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private Feedback feedback;

    @PrePersist
    void prePersist() {
        if (dtCriacao == null) {
            dtCriacao = LocalDateTime.now();
        }
        if (modo == null) {
            modo = ModoVerificacao.ONLINE;
        }
        if (status == null) {
            status = StatusVerificacao.PENDENTE;
        }
    }
}
