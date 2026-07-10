package com.personaflow.commerce.inventory.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.entity.InventoryStockEntity;
import com.personaflow.commerce.inventory.mapper.InventoryStockMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisStockService {

    public static final String AVAILABLE_KEY_PREFIX = "inventory:stock:available:";
    public static final String LOCKED_KEY_PREFIX = "inventory:stock:locked:";
    public static final String SOLD_KEY_PREFIX = "inventory:stock:sold:";
    public static final String RESERVATION_KEY_PREFIX = "inventory:reservation:";

    private static final long RESULT_SUCCESS = 1L;
    private static final long RESULT_ALREADY_RESERVED = 2L;
    private static final long RESULT_STOCK_NOT_ENOUGH = -1L;
    private static final long RESULT_STOCK_NOT_INITIALIZED = -2L;
    private static final long RESULT_STOCK_STATE_INVALID = -3L;
    private static final long RESULT_RESERVATION_CONFLICT = -4L;
    private static final String RESERVATION_METADATA_PREFIX = "__";

    private static final RedisScript<Long> INITIALIZE_STOCK_SCRIPT = script(
            "redis/inventory/initialize-stock.lua"
    );
    private static final RedisScript<Long> SYNCHRONIZE_STOCK_SCRIPT = script(
            "redis/inventory/synchronize-stock.lua"
    );
    private static final RedisScript<Long> RESERVE_STOCK_SCRIPT = script(
            "redis/inventory/reserve-stock.lua"
    );
    private static final RedisScript<Long> RELEASE_RESERVATION_SCRIPT = script(
            "redis/inventory/release-reservation.lua"
    );
    private static final RedisScript<Long> RELEASE_LOCKED_STOCK_SCRIPT = script(
            "redis/inventory/release-locked-stock.lua"
    );
    private static final RedisScript<Long> CONFIRM_SOLD_STOCK_SCRIPT = script(
            "redis/inventory/confirm-sold-stock.lua"
    );

    private final StringRedisTemplate redisTemplate;
    private final InventoryStockMapper inventoryStockMapper;
    private final Map<Long, Object> initializationLocks = new ConcurrentHashMap<>();

    public RedisStockService(
            StringRedisTemplate redisTemplate,
            InventoryStockMapper inventoryStockMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.inventoryStockMapper = inventoryStockMapper;
    }

    public void reserveStock(Long skuId, Integer quantity, String reservationId) {
        validateRequest(skuId, quantity, reservationId);
        long result = executeReserve(skuId, quantity, reservationId);
        if (result == RESULT_STOCK_NOT_INITIALIZED) {
            initializeFromDatabaseIfNeeded(skuId);
            result = executeReserve(skuId, quantity, reservationId);
        }

        if (result == RESULT_SUCCESS || result == RESULT_ALREADY_RESERVED) {
            return;
        }
        if (result == RESULT_STOCK_NOT_ENOUGH) {
            throw new BusinessException(ErrorCode.TRADE_STOCK_NOT_ENOUGH);
        }
        if (result == RESULT_RESERVATION_CONFLICT) {
            throw new BusinessException(ErrorCode.TRADE_STOCK_RESERVATION_FAILED);
        }
        throw new BusinessException(ErrorCode.TRADE_STOCK_SERVICE_UNAVAILABLE);
    }

    public void releaseReservation(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return;
        }
        String reservationKey = reservationKey(reservationId);
        Map<Object, Object> entries;
        try {
            entries = redisTemplate.opsForHash().entries(reservationKey);
        } catch (RuntimeException exception) {
            throw stockServiceUnavailable();
        }
        if (entries.isEmpty()) {
            return;
        }

        for (Object field : entries.keySet()) {
            String skuField = String.valueOf(field);
            if (skuField.startsWith(RESERVATION_METADATA_PREFIX)) {
                continue;
            }
            Long skuId;
            try {
                skuId = Long.valueOf(skuField);
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.TRADE_STOCK_RESERVATION_FAILED);
            }
            long result = execute(
                    RELEASE_RESERVATION_SCRIPT,
                    List.of(availableKey(skuId), lockedKey(skuId), reservationKey),
                    skuField
            );
            if (result != RESULT_SUCCESS && result != 0L) {
                throw new BusinessException(ErrorCode.TRADE_STOCK_RESERVATION_FAILED);
            }
        }
        delete(reservationKey);
    }

    public void confirmReservation(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return;
        }
        delete(reservationKey(reservationId));
    }

    public void releaseLockedStock(Long skuId, Integer quantity) {
        validateQuantity(skuId, quantity);
        long result = execute(
                RELEASE_LOCKED_STOCK_SCRIPT,
                List.of(availableKey(skuId), lockedKey(skuId)),
                quantity.toString()
        );
        handlePostCommitTransitionResult(skuId, result);
    }

    public void confirmSoldStock(Long skuId, Integer quantity) {
        validateQuantity(skuId, quantity);
        long result = execute(
                CONFIRM_SOLD_STOCK_SCRIPT,
                List.of(lockedKey(skuId), soldKey(skuId)),
                quantity.toString()
        );
        handlePostCommitTransitionResult(skuId, result);
    }

    public void initializeStockIfAbsent(InventoryStockEntity stock) {
        validateStock(stock);
        execute(
                INITIALIZE_STOCK_SCRIPT,
                stockKeys(stock.getSkuId()),
                stock.getAvailableQuantity().toString(),
                stock.getLockedQuantity().toString(),
                stock.getSoldQuantity().toString()
        );
    }

    public void synchronizeStock(InventoryStockEntity stock) {
        validateStock(stock);
        execute(
                SYNCHRONIZE_STOCK_SCRIPT,
                stockKeys(stock.getSkuId()),
                stock.getAvailableQuantity().toString(),
                stock.getLockedQuantity().toString(),
                stock.getSoldQuantity().toString()
        );
    }

    private long executeReserve(Long skuId, Integer quantity, String reservationId) {
        return execute(
                RESERVE_STOCK_SCRIPT,
                List.of(availableKey(skuId), lockedKey(skuId), reservationKey(reservationId)),
                skuId.toString(),
                quantity.toString(),
                Long.toString(Instant.now().toEpochMilli())
        );
    }

    private void initializeFromDatabaseIfNeeded(Long skuId) {
        Object lock = initializationLocks.computeIfAbsent(skuId, ignored -> new Object());
        synchronized (lock) {
            if (isStockInitialized(skuId)) {
                return;
            }
            InventoryStockEntity stock = inventoryStockMapper.selectBySkuId(skuId);
            if (stock == null) {
                throw new BusinessException(ErrorCode.TRADE_STOCK_NOT_FOUND);
            }
            initializeStockIfAbsent(stock);
        }
    }

    private void handlePostCommitTransitionResult(Long skuId, long result) {
        if (result == RESULT_SUCCESS) {
            return;
        }
        if (result == RESULT_STOCK_NOT_INITIALIZED || result == RESULT_STOCK_STATE_INVALID) {
            synchronizeFromDatabase(skuId);
            return;
        }
        throw new BusinessException(ErrorCode.TRADE_STOCK_SERVICE_UNAVAILABLE);
    }

    private void synchronizeFromDatabase(Long skuId) {
        Object lock = initializationLocks.computeIfAbsent(skuId, ignored -> new Object());
        synchronized (lock) {
            InventoryStockEntity stock = inventoryStockMapper.selectBySkuId(skuId);
            if (stock == null) {
                throw new BusinessException(ErrorCode.TRADE_STOCK_NOT_FOUND);
            }
            synchronizeStock(stock);
        }
    }

    private boolean isStockInitialized(Long skuId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(availableKey(skuId)))
                    && Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey(skuId)))
                    && Boolean.TRUE.equals(redisTemplate.hasKey(soldKey(skuId)));
        } catch (RuntimeException exception) {
            throw stockServiceUnavailable();
        }
    }

    private long execute(RedisScript<Long> script, List<String> keys, String... args) {
        Long result;
        try {
            result = redisTemplate.execute(script, keys, (Object[]) args);
        } catch (RuntimeException exception) {
            throw stockServiceUnavailable();
        }
        if (result == null) {
            throw stockServiceUnavailable();
        }
        return result;
    }

    private void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException exception) {
            throw stockServiceUnavailable();
        }
    }

    private void validateRequest(Long skuId, Integer quantity, String reservationId) {
        validateQuantity(skuId, quantity);
        if (reservationId == null || reservationId.isBlank()) {
            throw new BusinessException(ErrorCode.TRADE_STOCK_RESERVATION_FAILED);
        }
    }

    private void validateQuantity(Long skuId, Integer quantity) {
        if (skuId == null || quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.TRADE_INVALID_QUANTITY);
        }
    }

    private void validateStock(InventoryStockEntity stock) {
        if (stock == null || stock.getSkuId() == null
                || stock.getAvailableQuantity() == null
                || stock.getLockedQuantity() == null
                || stock.getSoldQuantity() == null) {
            throw new BusinessException(ErrorCode.TRADE_STOCK_STATE_INVALID);
        }
    }

    private List<String> stockKeys(Long skuId) {
        return List.of(availableKey(skuId), lockedKey(skuId), soldKey(skuId));
    }

    public String availableKey(Long skuId) {
        return AVAILABLE_KEY_PREFIX + "{" + skuId + "}";
    }

    public String lockedKey(Long skuId) {
        return LOCKED_KEY_PREFIX + "{" + skuId + "}";
    }

    public String soldKey(Long skuId) {
        return SOLD_KEY_PREFIX + "{" + skuId + "}";
    }

    public String reservationKey(String reservationId) {
        return RESERVATION_KEY_PREFIX + "{" + reservationId + "}";
    }

    private BusinessException stockServiceUnavailable() {
        return new BusinessException(ErrorCode.TRADE_STOCK_SERVICE_UNAVAILABLE);
    }

    private static RedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
