package com.personaflow.commerce.behavior.messaging;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BehaviorRoutingKeyResolverTest {

    private final BehaviorRoutingKeyResolver resolver = new BehaviorRoutingKeyResolver();

    @Test
    void resolvesRoutingKeysForAllBehaviorEventTypes() {
        assertThat(resolver.resolve(BehaviorEventType.PRODUCT_VIEW)).isEqualTo("behavior.product.view");
        assertThat(resolver.resolve(BehaviorEventType.PRODUCT_SEARCH)).isEqualTo("behavior.product.search");
        assertThat(resolver.resolve(BehaviorEventType.FAVORITE_ADD)).isEqualTo("behavior.favorite.add");
        assertThat(resolver.resolve(BehaviorEventType.FAVORITE_REMOVE)).isEqualTo("behavior.favorite.remove");
        assertThat(resolver.resolve(BehaviorEventType.CART_ADD)).isEqualTo("behavior.cart.add");
        assertThat(resolver.resolve(BehaviorEventType.CART_REMOVE)).isEqualTo("behavior.cart.remove");
        assertThat(resolver.resolve(BehaviorEventType.CART_CLEAR)).isEqualTo("behavior.cart.clear");
        assertThat(resolver.resolve(BehaviorEventType.ORDER_CREATED)).isEqualTo("behavior.order.created");
        assertThat(resolver.resolve(BehaviorEventType.PAYMENT_SUCCESS)).isEqualTo("behavior.payment.success");
        assertThat(resolver.resolve(BehaviorEventType.ORDER_CANCELED)).isEqualTo("behavior.order.canceled");
    }

    @Test
    void rejectsUnsupportedEventType() {
        assertThatThrownBy(() -> resolver.resolve("UNKNOWN_EVENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported behavior event type");
    }
}
