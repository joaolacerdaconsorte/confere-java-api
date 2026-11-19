package br.com.confere.messaging;

import br.com.confere.domain.entity.AlertaGuardiao;
import br.com.confere.domain.entity.Verificacao;
import br.com.confere.domain.entity.VinculoGuardiao;
import br.com.confere.domain.enums.CanalEnvioAlerta;
import br.com.confere.domain.enums.NivelMinimoAlerta;
import br.com.confere.domain.enums.StatusAlerta;
import br.com.confere.domain.enums.StatusVinculo;
import br.com.confere.domain.enums.Veredito;
import br.com.confere.repository.AlertaGuardiaoRepository;
import br.com.confere.repository.VerificacaoRepository;
import br.com.confere.repository.VinculoGuardiaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consumidor da fila confere.verificacao.risco (RN07).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertaGuardiaoListener {

    private final VerificacaoRepository verificacaoRepository;
    private final VinculoGuardiaoRepository vinculoRepository;
    private final AlertaGuardiaoRepository alertaRepository;

    @RabbitListener(queues = "${confere.rabbitmq.queue-risco:confere.verificacao.risco}")
    @Transactional
    public void processarEventoRisco(VerificacaoRiscoEvent event) {
        Verificacao verificacao = verificacaoRepository.findById(event.verificacaoId()).orElse(null);
        if (verificacao == null) {
            log.warn("Verificação {} não encontrada para gerar alertas ao guardião.", event.verificacaoId());
            return;
        }

        List<VinculoGuardiao> vinculosAtivos = vinculoRepository
                .findByProtegidoIdAndStatus(event.usuarioId(), StatusVinculo.ATIVO);

        for (VinculoGuardiao vinculo : vinculosAtivos) {
            if (!deveAlertar(event.veredito(), vinculo.getNivelMinimo())) {
                continue;
            }
            if (alertaRepository.existsByVerificacaoIdAndVinculoId(verificacao.getId(), vinculo.getId())) {
                continue;
            }

            AlertaGuardiao alerta = AlertaGuardiao.builder()
                    .verificacao(verificacao)
                    .vinculo(vinculo)
                    .canalEnvio(CanalEnvioAlerta.PUSH)
                    .status(StatusAlerta.ENVIADO)
                    .build();
            alertaRepository.save(alerta);

            String pushToken = vinculo.getGuardiao().getExpoPushToken();
            if (pushToken != null && !pushToken.isBlank()) {
                log.info("Push enviado para EXPO_PUSH_TOKEN={} | guardiao={} | verificacao={}",
                        pushToken, vinculo.getGuardiao().getEmail(), verificacao.getId());
            }
        }
    }

    public static boolean deveAlertar(Veredito veredito, NivelMinimoAlerta nivelMinimo) {
        if (veredito == Veredito.GOLPE) {
            return true;
        }
        if (veredito == Veredito.SUSPEITO) {
            return nivelMinimo == NivelMinimoAlerta.SUSPEITO;
        }
        return false;
    }
}
