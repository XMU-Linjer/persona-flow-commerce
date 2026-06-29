package com.personaflow.commerce.behavior.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorRabbitConfigTest {

    private final BehaviorRabbitConfig config = new BehaviorRabbitConfig();

    @Test
    void declaresDurableBehaviorExchangeAndQueues() {
        TopicExchange exchange = config.behaviorExchange();
        Queue persistQueue = config.behaviorPersistQueue();
        Queue deadQueue = config.behaviorDeadQueue();

        assertThat(exchange.getName()).isEqualTo(BehaviorRabbitConfig.BEHAVIOR_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(persistQueue.getName()).isEqualTo(BehaviorRabbitConfig.BEHAVIOR_PERSIST_QUEUE);
        assertThat(persistQueue.isDurable()).isTrue();
        assertThat(persistQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", BehaviorRabbitConfig.BEHAVIOR_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", BehaviorRabbitConfig.BEHAVIOR_DEAD_ROUTING_KEY);
        assertThat(deadQueue.getName()).isEqualTo(BehaviorRabbitConfig.BEHAVIOR_DEAD_QUEUE);
        assertThat(deadQueue.isDurable()).isTrue();
    }

    @Test
    void bindsPersistQueueToAllBehaviorRoutingKeysAndDeadQueueToDeadRoutingKey() {
        Declarables declarables = config.behaviorQueueBindings(
                config.behaviorExchange(),
                config.behaviorPersistQueue(),
                config.behaviorDeadQueue()
        );

        List<Binding> bindings = declarables.getDeclarables().stream()
                .filter(Binding.class::isInstance)
                .map(Binding.class::cast)
                .toList();

        assertThat(bindings).hasSize(BehaviorRabbitConfig.BEHAVIOR_ROUTING_KEYS.size() + 1);
        assertThat(bindings)
                .extracting(Binding::getRoutingKey)
                .containsAll(BehaviorRabbitConfig.BEHAVIOR_ROUTING_KEYS)
                .contains(BehaviorRabbitConfig.BEHAVIOR_DEAD_ROUTING_KEY);
    }
}
