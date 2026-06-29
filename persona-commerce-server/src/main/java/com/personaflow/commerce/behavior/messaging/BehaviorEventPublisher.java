package com.personaflow.commerce.behavior.messaging;

public interface BehaviorEventPublisher {

    void publish(BehaviorEventMessage message);
}
