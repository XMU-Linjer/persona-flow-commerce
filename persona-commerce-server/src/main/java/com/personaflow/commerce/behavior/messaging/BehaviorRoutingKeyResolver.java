package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import org.springframework.stereotype.Component;

@Component
public class BehaviorRoutingKeyResolver {

    public String resolve(String eventType) {
        try {
            return resolve(BehaviorEventType.valueOf(eventType));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unsupported behavior event type: " + eventType, exception);
        }
    }

    public String resolve(BehaviorEventType eventType) {
        return switch (eventType) {
            case PRODUCT_VIEW -> "behavior.product.view";
            case PRODUCT_SEARCH -> "behavior.product.search";
            case FAVORITE_ADD -> "behavior.favorite.add";
            case FAVORITE_REMOVE -> "behavior.favorite.remove";
            case CART_ADD -> "behavior.cart.add";
            case CART_REMOVE -> "behavior.cart.remove";
            case CART_CLEAR -> "behavior.cart.clear";
            case ORDER_CREATED -> "behavior.order.created";
            case PAYMENT_SUCCESS -> "behavior.payment.success";
            case ORDER_CANCELED -> "behavior.order.canceled";
        };
    }
}
