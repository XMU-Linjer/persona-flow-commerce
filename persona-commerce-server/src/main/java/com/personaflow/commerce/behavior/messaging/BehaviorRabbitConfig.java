package com.personaflow.commerce.behavior.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration
@EnableRabbit
public class BehaviorRabbitConfig {

    public static final String BEHAVIOR_EXCHANGE = "commerce.behavior.exchange";
    public static final String BEHAVIOR_PERSIST_QUEUE = "behavior.persist.queue";
    public static final String BEHAVIOR_DEAD_QUEUE = "behavior.dead.queue";
    public static final String BEHAVIOR_DEAD_ROUTING_KEY = "behavior.dead";

    public static final Set<String> BEHAVIOR_ROUTING_KEYS = Set.of(
            "behavior.product.view",
            "behavior.product.search",
            "behavior.favorite.add",
            "behavior.favorite.remove",
            "behavior.cart.add",
            "behavior.cart.remove",
            "behavior.cart.clear",
            "behavior.order.created",
            "behavior.payment.success",
            "behavior.order.canceled"
    );

    @Bean
    public TopicExchange behaviorExchange() {
        return ExchangeBuilder.topicExchange(BEHAVIOR_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue behaviorPersistQueue() {
        return QueueBuilder.durable(BEHAVIOR_PERSIST_QUEUE)
                .withArgument("x-dead-letter-exchange", BEHAVIOR_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", BEHAVIOR_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue behaviorDeadQueue() {
        return QueueBuilder.durable(BEHAVIOR_DEAD_QUEUE).build();
    }

    @Bean
    public Declarables behaviorQueueBindings(
            TopicExchange behaviorExchange,
            Queue behaviorPersistQueue,
            Queue behaviorDeadQueue
    ) {
        List<Declarable> declarables = new ArrayList<>();
        for (String routingKey : BEHAVIOR_ROUTING_KEYS) {
            declarables.add(BindingBuilder.bind(behaviorPersistQueue).to(behaviorExchange).with(routingKey));
        }
        declarables.add(BindingBuilder.bind(behaviorDeadQueue).to(behaviorExchange).with(BEHAVIOR_DEAD_ROUTING_KEY));
        return new Declarables(declarables);
    }

    @Bean
    public Jackson2JsonMessageConverter behaviorJackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory behaviorRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter behaviorJackson2JsonMessageConverter,
            @Value("${commerce.behavior.rabbit.listener.enabled:true}") boolean listenerEnabled
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(behaviorJackson2JsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        factory.setAutoStartup(listenerEnabled);
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 5000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build());
        return factory;
    }
}
