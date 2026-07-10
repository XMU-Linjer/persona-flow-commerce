package com.personaflow.commerce.inventory.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.entity.InventoryStockEntity;
import com.personaflow.commerce.inventory.mapper.InventoryStockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private InventoryStockMapper inventoryStockMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RedisStockService redisStockService;

    @BeforeEach
    void setUp() {
        redisStockService = new RedisStockService(redisTemplate, inventoryStockMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reserveStockSucceedsThroughLuaWithoutReadingMysql() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        redisStockService.reserveStock(30001L, 2, "ORDER-1");

        verify(inventoryStockMapper, never()).selectBySkuId(30001L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reserveStockRejectsInsufficientRedisStockBeforeMysqlTransaction() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(-1L);

        assertBusinessError(
                () -> redisStockService.reserveStock(30001L, 2, "ORDER-1"),
                ErrorCode.TRADE_STOCK_NOT_ENOUGH
        );
        verify(inventoryStockMapper, never()).selectBySkuId(30001L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reserveStockLazilyInitializesMissingRedisStockFromMysqlAndRetries() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(-2L, 1L, 1L);
        when(redisTemplate.hasKey(any())).thenReturn(false);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(stock());

        redisStockService.reserveStock(30001L, 2, "ORDER-1");

        verify(inventoryStockMapper).selectBySkuId(30001L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reserveStockFailsClosedWhenRedisIsUnavailable() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("redis down"));

        assertBusinessError(
                () -> redisStockService.reserveStock(30001L, 2, "ORDER-1"),
                ErrorCode.TRADE_STOCK_SERVICE_UNAVAILABLE
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void releaseReservationRestoresEveryReservedSkuAndDeletesMarker() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("__createdAt", "1");
        entries.put("30001", "2");
        entries.put("30002", "1");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("inventory:reservation:{ORDER-1}")).thenReturn(entries);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);

        redisStockService.releaseReservation("ORDER-1");

        verify(redisTemplate).delete("inventory:reservation:{ORDER-1}");
    }

    @Test
    @SuppressWarnings("unchecked")
    void postCommitTransitionResynchronizesFromMysqlWhenRedisStateIsMissing() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(-2L, 1L);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(stock());

        redisStockService.releaseLockedStock(30001L, 2);

        verify(inventoryStockMapper).selectBySkuId(30001L);
    }

    @Test
    void keyNamesKeepEachSkuStockStateExplicit() {
        assertThat(redisStockService.availableKey(30001L)).isEqualTo("inventory:stock:available:{30001}");
        assertThat(redisStockService.lockedKey(30001L)).isEqualTo("inventory:stock:locked:{30001}");
        assertThat(redisStockService.soldKey(30001L)).isEqualTo("inventory:stock:sold:{30001}");
        assertThat(redisStockService.reservationKey("ORDER-1")).isEqualTo("inventory:reservation:{ORDER-1}");
    }

    private InventoryStockEntity stock() {
        InventoryStockEntity stock = new InventoryStockEntity();
        stock.setId(1L);
        stock.setSkuId(30001L);
        stock.setAvailableQuantity(100);
        stock.setLockedQuantity(0);
        stock.setSoldQuantity(0);
        return stock;
    }

    private void assertBusinessError(Runnable operation, ErrorCode expectedErrorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(expectedErrorCode);
    }
}
