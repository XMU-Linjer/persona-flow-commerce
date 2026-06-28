package com.personaflow.commerce.product.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaflow.commerce.product.vo.ProductDetailVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class ProductCacheService {

    public static final Duration PRODUCT_DETAIL_TTL = Duration.ofHours(1);
    public static final String PRODUCT_DETAIL_KEY_PREFIX = "catalog:product:detail:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ProductCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<ProductDetailVO> getProductDetail(Long spuId) {
        String key = productDetailKey(spuId);
        try {
            String cachedJson = redisTemplate.opsForValue().get(key);
            if (cachedJson == null || cachedJson.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(cachedJson, ProductDetailVO.class));
        } catch (JsonProcessingException exception) {
            deleteQuietly(key);
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public void putProductDetail(Long spuId, ProductDetailVO productDetail) {
        try {
            String key = productDetailKey(spuId);
            String json = objectMapper.writeValueAsString(productDetail);
            redisTemplate.opsForValue().set(key, json, PRODUCT_DETAIL_TTL);
        } catch (JsonProcessingException | RuntimeException exception) {
            // Redis cache is best effort; MySQL remains the source of truth.
        }
    }

    public String productDetailKey(Long spuId) {
        return PRODUCT_DETAIL_KEY_PREFIX + spuId;
    }

    private void deleteQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            // Ignore cleanup failures and fall back to MySQL.
        }
    }
}
