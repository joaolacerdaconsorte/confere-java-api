package br.com.confere.config;

import br.com.confere.domain.entity.*;
import br.com.confere.domain.enums.FinalidadeConsentimento;
import br.com.confere.domain.enums.NivelMinimoAlerta;
import br.com.confere.domain.enums.PerfilUsuario;
import br.com.confere.domain.enums.StatusVinculo;
import br.com.confere.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataSeederConfig implements CommandLineRunner {

    private final MunicipioRepository municipioRepository;
    private final CanalOrigemRepository canalOrigemRepository;
    private final TipoGolpeRepository tipoGolpeRepository;
    private final RegraOfflineRepository regraOfflineRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConsentimentoRepository consentimentoRepository;
    private final VinculoGuardiaoRepository vinculoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedMunicipios();
        seedCanais();
        seedTiposGolpe();
        seedRegrasOffline();
        seedUsuariosEVinculos();
    }

    private void seedMunicipios() {
        if (municipioRepository.count() > 0) {
            return;
        }
        municipioRepository.saveAll(List.of(
                Municipio.builder().nome("São Paulo").uf("SP").codIbge(3550308).build(),
                Municipio.builder().nome("Rio de Janeiro").uf("RJ").codIbge(3304557).build(),
                Municipio.builder().nome("Belo Horizonte").uf("MG").codIbge(3106200).build(),
                Municipio.builder().nome("Campinas").uf("SP").codIbge(3509502).build(),
                Municipio.builder().nome("Salvador").uf("BA").codIbge(2927408).build()
        ));
    }

    private void seedCanais() {
        if (canalOrigemRepository.count() > 0) {
            return;
        }
        canalOrigemRepository.saveAll(List.of(
                CanalOrigem.builder().codigo("WHATSAPP").nome("WhatsApp").build(),
                CanalOrigem.builder().codigo("SMS").nome("SMS").build(),
                CanalOrigem.builder().codigo("LIGACAO").nome("Ligação telefônica").build(),
                CanalOrigem.builder().codigo("EMAIL").nome("E-mail").build(),
                CanalOrigem.builder().codigo("CARTA").nome("Carta ou boleto físico").build(),
                CanalOrigem.builder().codigo("REDE_SOCIAL").nome("Rede social").build()
        ));
    }

    private void seedTiposGolpe() {
        if (tipoGolpeRepository.count() > 0) {
            return;
        }
        tipoGolpeRepository.saveAll(List.of(
                TipoGolpe.builder().codigo("FALSA_PROVA_VIDA").nome("Falsa prova de vida (pede selfie ou documento)")
                        .descricao("Golpistas pedem selfie ou documentos pelo WhatsApp simulando o INSS.")
                        .gravidade(5).orgaoImitado("INSS").build(),
                TipoGolpe.builder().codigo("FALSO_REEMBOLSO_INSS").nome("Falsa devolução de descontos do INSS")
                        .descricao("Promessa falsa de devolução imediata mediante link ou taxa.")
                        .gravidade(5).orgaoImitado("INSS").build(),
                TipoGolpe.builder().codigo("FALSA_CENTRAL").nome("Falsa central do banco")
                        .descricao("Ligação ou mensagem fingindo ser a central de segurança do banco.")
                        .gravidade(5).orgaoImitado("Bancos").build(),
                TipoGolpe.builder().codigo("FALSO_PARENTE").nome("\"Oi mãe, troquei de número\"")
                        .descricao("Pessoa se passa por familiar pedindo Pix com urgência.")
                        .gravidade(4).orgaoImitado("Família").build(),
                TipoGolpe.builder().codigo("FALSO_VALORES_RECEBER").nome("Falso resgate de valores esquecidos")
                        .descricao("Site falso cobrando taxa para liberar valores do Sistema Valores a Receber.")
                        .gravidade(4).orgaoImitado("Banco Central").build(),
                TipoGolpe.builder().codigo("FALSO_EMPRESTIMO").nome("Empréstimo consignado falso ou não autorizado")
                        .descricao("Oferta enganosa ou pedido de selfie/senha para cancelar consignado.")
                        .gravidade(4).orgaoImitado("Bancos / INSS").build(),
                TipoGolpe.builder().codigo("DEEPFAKE_CELEBRIDADE").nome("Vídeo falso de jornalista ou famoso")
                        .descricao("Vídeo manipulado prometendo benefício ou indenização.")
                        .gravidade(4).orgaoImitado("Imprensa").build(),
                TipoGolpe.builder().codigo("FALSO_BOLETO").nome("Boleto ou conta adulterada")
                        .descricao("Boleto com código de barras ou QR Code Pix alterado.")
                        .gravidade(3).orgaoImitado("Concessionárias").build(),
                TipoGolpe.builder().codigo("LINK_RASTREIO").nome("Falsa taxa de entrega ou rastreio")
                        .descricao("SMS ou e-mail cobrando taxa para liberar encomenda.")
                        .gravidade(3).orgaoImitado("Correios").build(),
                TipoGolpe.builder().codigo("PIX_ERRADO").nome("\"Te mandei um Pix por engano\"")
                        .descricao("Pedido de devolução de Pix falso para outra conta.")
                        .gravidade(3).orgaoImitado(null).build()
        ));
    }

    private void seedRegrasOffline() {
        if (regraOfflineRepository.count() > 0) {
            return;
        }
        regraOfflineRepository.saveAll(List.of(
                RegraOffline.builder().padrao("(?i)(selfie|foto do rosto|biometria facial)").tipoSinal("PEDIDO_SELFIE")
                        .peso(new BigDecimal("0.55")).mensagem("Pedido de selfie ou biometria por mensagem é sinal clássico de golpe.")
                        .versao(1).ativo(true).build(),
                RegraOffline.builder().padrao("(?i)(senha|token|código de segurança|pin)").tipoSinal("PEDIDO_SENHA")
                        .peso(new BigDecimal("0.60")).mensagem("Órgãos públicos e bancos nunca pedem sua senha ou token.")
                        .versao(1).ativo(true).build(),
                RegraOffline.builder().padrao("(?i)(vence hoje|último dia|bloqueado|suspenso|urgente)").tipoSinal("URGENCIA")
                        .peso(new BigDecimal("0.30")).mensagem("Pressa exagerada para agir hoje costuma ser usada para enganar.")
                        .versao(1).ativo(true).build(),
                RegraOffline.builder().padrao("(?i)(bit\\.ly|tinyurl|\\.xyz|\\.top|clique aqui)").tipoSinal("LINK_SUSPEITO")
                        .peso(new BigDecimal("0.35")).mensagem("Links encurtados ou fora do domínio gov.br exigem atenção.")
                        .versao(1).ativo(true).build(),
                RegraOffline.builder().padrao("(?i)(pix|taxa de liberação|pague agora|transferência)").tipoSinal("CHAVE_PIX")
                        .peso(new BigDecimal("0.35")).mensagem("Cobrança via Pix para liberar benefício ou entrega é suspeita.")
                        .versao(1).ativo(true).build(),
                RegraOffline.builder().padrao("(?i)(inss|receita federal|banco central|correios|gov\\.br)").tipoSinal("ORGAO_OFICIAL_CITADO")
                        .peso(new BigDecimal("0.15")).mensagem("A mensagem cita órgão oficial: confirme sempre pelo canal oficial.")
                        .versao(1).ativo(true).build()
        ));
    }

    private void seedUsuariosEVinculos() {
        if (usuarioRepository.count() > 0) {
            return;
        }
        Municipio sp = municipioRepository.findByCodIbge(3550308).orElseThrow();
        String senhaHash = passwordEncoder.encode("Senha@123");

        Usuario cida = usuarioRepository.save(Usuario.builder()
                .nome("Maria Aparecida")
                .email("cida@confere.app")
                .telefone("11987654321")
                .senhaHash(senhaHash)
                .perfil(PerfilUsuario.CIDADAO)
                .dtNascimento(LocalDate.of(1956, 3, 14))
                .municipio(sp)
                .fonteGrande(true)
                .audioAtivo(true)
                .ativo(true)
                .build());

        Usuario joao = usuarioRepository.save(Usuario.builder()
                .nome("João Aparecido")
                .email("joao@confere.app")
                .telefone("11991234567")
                .senhaHash(senhaHash)
                .perfil(PerfilUsuario.CIDADAO)
                .dtNascimento(LocalDate.of(1985, 7, 20))
                .municipio(sp)
                .fonteGrande(false)
                .audioAtivo(true)
                .expoPushToken("ExponentPushToken[demo-joao-001]")
                .ativo(true)
                .build());

        usuarioRepository.save(Usuario.builder()
                .nome("Administrador Confere")
                .email("admin@confere.app")
                .telefone("11990000000")
                .senhaHash(senhaHash)
                .perfil(PerfilUsuario.ADMIN)
                .dtNascimento(LocalDate.of(1990, 1, 10))
                .municipio(sp)
                .fonteGrande(false)
                .audioAtivo(false)
                .ativo(true)
                .build());

        consentimentoRepository.saveAll(List.of(
                Consentimento.builder().usuario(cida).finalidade(FinalidadeConsentimento.ANALISE_IA)
                        .versaoTermo("1.0").aceito(true).build(),
                Consentimento.builder().usuario(cida).finalidade(FinalidadeConsentimento.ALERTA_GUARDIAO)
                        .versaoTermo("1.0").aceito(true).build(),
                Consentimento.builder().usuario(joao).finalidade(FinalidadeConsentimento.ANALISE_IA)
                        .versaoTermo("1.0").aceito(true).build()
        ));

        vinculoRepository.save(VinculoGuardiao.builder()
                .protegido(cida)
                .guardiao(joao)
                .parentesco("Filho")
                .nivelMinimo(NivelMinimoAlerta.GOLPE)
                .status(StatusVinculo.ATIVO)
                .dtAceite(LocalDateTime.now())
                .build());
    }
}
