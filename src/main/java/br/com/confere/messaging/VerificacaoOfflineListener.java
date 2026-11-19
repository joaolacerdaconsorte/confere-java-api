package br.com.confere.messaging;

import br.com.confere.service.VerificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor da fila confere.verificacao.offline (RN08).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificacaoOfflineListener {

    private final VerificacaoService verificacaoService;

    @RabbitListener(queues = "${confere.rabbitmq.queue-offline:confere.verificacao.offline}")
    public void processarVerificacaoOffline(VerificacaoOfflineEvent event) {
        log.info("Processando verificação offline da fila: verificacaoId={}", event.verificacaoId());
        verificacaoService.processarOffline(event.verificacaoId());
    }
}
