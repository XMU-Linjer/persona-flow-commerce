package com.personaflow.commerce.product.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaflow.commerce.product.vo.ProductDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private ProductCacheService productCacheService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        productCacheService = new ProductCacheService(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getProductDetailReturnsCachedValue() throws Exception {
        ProductDetailVO productDetail = productDetailVO(20001L);
        when(valueOperations.get("catalog:product:detail:20001"))
                .thenReturn(objectMapper.writeValueAsString(productDetail));

        Optional<ProductDetailVO> result = productCacheService.getProductDetail(20001L);

        assertThat(result).isPresent();
        assertThat(result.get().spuId()).isEqualTo(20001L);
        assertThat(result.get().attributes()).containsEntry("连接方式", "蓝牙");
    }

    @Test
    void getProductDetailReturnsEmptyWhenCacheMisses() {
        when(valueOperations.get("catalog:product:detail:20001")).thenReturn(null);

        Optional<ProductDetailVO> result = productCacheService.getProductDetail(20001L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductDetailFallsBackWhenRedisReadFails() {
        when(valueOperations.get("catalog:product:detail:20001"))
                .thenThrow(new RuntimeException("redis unavailable"));

        Optional<ProductDetailVO> result = productCacheService.getProductDetail(20001L);

        assertThat(result).isEmpty();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void getProductDetailDeletesInvalidJsonAndFallsBack() {
        when(valueOperations.get("catalog:product:detail:20001")).thenReturn("{invalid-json");

        Optional<ProductDetailVO> result = productCacheService.getProductDetail(20001L);

        assertThat(result).isEmpty();
        verify(redisTemplate).delete("catalog:product:detail:20001");
    }

    @Test
    void putProductDetailWritesJsonWithTtl() throws Exception {
        ProductDetailVO productDetail = productDetailVO(20001L);
        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);

        productCacheService.putProductDetail(20001L, productDetail);

        verify(valueOperations).set(
                eq("catalog:product:detail:20001"),
                jsonCaptor.capture(),
                eq(ProductCacheService.PRODUCT_DETAIL_TTL)
        );
        ProductDetailVO cached = objectMapper.readValue(jsonCaptor.getValue(), ProductDetailVO.class);
        assertThat(cached.spuId()).isEqualTo(20001L);
    }

    @Test
    void putProductDetailIgnoresRedisWriteFailure() {
        ProductDetailVO productDetail = productDetailVO(20001L);
        doThrow(new RuntimeException("redis unavailable"))
                .when(valueOperations)
                .set(eq("catalog:product:detail:20001"), anyString(), eq(ProductCacheService.PRODUCT_DETAIL_TTL));

        assertThatCode(() -> productCacheService.putProductDetail(20001L, productDetail))
                .doesNotThrowAnyException();
    }

    private ProductDetailVO productDetailVO(Long spuId) {
        return new ProductDetailVO(
                spuId,
                201L,
                "键盘鼠标",
                "KeyForge K3",
                "KeyForge K3 subtitle",
                "KeyForge",
                "Mechanical keyboard",
                "main.jpg",
                List.of("detail-1.jpg"),
                Map.of("连接方式", "蓝牙"),
                "办公,演示",
                List.of()
        );
    }
}
