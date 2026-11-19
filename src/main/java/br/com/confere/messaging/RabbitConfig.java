package br.com.confere.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${confere.rabbitmq.exchange:confere.verificacoes}")
    private String exchangeName;

    @Value("${confere.rabbitmq.queue-risco:confere.verificacao.risco}")
    private String queueRisco;

    @Value("${confere.rabbitmq.routing-key-risco:verificacao.risco}")
    private String routingKeyRisco;

    @Value("${confere.rabbitmq.queue-offline:confere.verificacao.offline}")
    private String queueOffline;

    @Value("${confere.rabbitmq.routing-key-offline:verificacao.offline}")
    private String routingKeyOffline;

    @Bean
    public TopicExchange confereVerificacoesExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Queue riscoDlq() {
        return QueueBuilder.durable(queueRisco + ".dlq").build();
    }

    @Bean
    public Queue riscoQueue() {
        return QueueBuilder.durable(queueRisco)
                .deadLetterExchange("")
                .deadLetterRoutingKey(queueRisco + ".dlq")
                .build();
    }

    @Bean
    public Binding riscoBinding(Queue riscoQueue, TopicExchange confereVerificacoesExchange) {
        return BindingBuilder.bind(riscoQueue).to(confereVerificacoesExchange).with(routingKeyRisco);
    }

    @Bean
    public Queue offlineDlq() {
        return QueueBuilder.durable(queueOffline + ".dlq").build();
    }

    @Bean
    public Queue offlineQueue() {
        return QueueBuilder.durable(queueOffline)
                .deadLetterExchange("")
                .deadLetterRoutingKey(queueOffline + ".dlq")
                .build();
    }

    @Bean
    public Binding offlineBinding(Queue offlineQueue, TopicExchange confereVerificacoesExchange) {
        return BindingBuilder.bind(offlineQueue).to(confereVerificacoesExchange).with(routingKeyOffline);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
