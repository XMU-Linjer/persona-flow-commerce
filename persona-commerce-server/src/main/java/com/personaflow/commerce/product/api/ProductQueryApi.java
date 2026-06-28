package com.personaflow.commerce.product.api;

import com.personaflow.commerce.product.api.model.ProductSnapshot;

import java.util.Collection;
import java.util.Map;

public interface ProductQueryApi {

    ProductSnapshot requireSellableSku(Long skuId);

    Map<Long, ProductSnapshot> requireSellableSkus(Collection<Long> skuIds);
}
