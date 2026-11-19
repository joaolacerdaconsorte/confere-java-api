package br.com.confere.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificacaoEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${confere.rabbitmq.exchange:confere.verificacoes}")
    private String exchange;

    @Value("${confere.rabbitmq.routing-key-risco:verificacao.risco}")
    private String routingKeyRisco;

    @Value("${confere.rabbitmq.routing-key-offline:verificacao.offline}")
    private String routingKeyOffline;

    /**
     * RN06: publica VerificacaoRiscoEvent após o commit da transação.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificacaoRisco(VerificacaoRiscoEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyRisco, event);
            log.info("Evento VerificacaoRiscoEvent publicado no RabbitMQ: verificacaoId={}, veredito={}",
                    event.verificacaoId(), event.veredito());
        } catch (Exception ex) {
            log.warn("Falha ao publicar VerificacaoRiscoEvent no RabbitMQ (verificacaoId={}): {}",
                    event.verificacaoId(), ex.getMessage());
        }
    }

    /**
     * RN08: publica VerificacaoOfflineEvent após o commit da transação.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificacaoOffline(VerificacaoOfflineEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKeyOffline, event);
            log.info("Evento VerificacaoOfflineEvent publicado no RabbitMQ: verificacaoId={}", event.verificacaoId());
        } catch (Exception ex) {
            log.warn("Falha ao publicar VerificacaoOfflineEvent no RabbitMQ (verificacaoId={}): {}",
                    event.verificacaoId(), ex.getMessage());
        }
    }
}
