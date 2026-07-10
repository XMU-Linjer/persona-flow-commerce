package com.personaflow.commerce.inventory.service;

import com.personaflow.commerce.PersonaCommerceServerApplication;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PersonaCommerceServerApplication.class)
class RedisStockConcurrencyTest {

    private static final long TEST_SKU_ID = 9_999_999L;
    private static final int INITIAL_STOCK = 100;
    private static final int ATTEMPTS = 1_000;
    private static final int WORKERS = 100;

    @Autowired
    private RedisStockService redisStockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void oneThousandReservationAttemptsUnderConcurrentLoadDoNotOversell() throws Exception {
        String availableKey = redisStockService.availableKey(TEST_SKU_ID);
        String lockedKey = redisStockService.lockedKey(TEST_SKU_ID);
        String soldKey = redisStockService.soldKey(TEST_SKU_ID);
        List<String> reservationKeys = new ArrayList<>(ATTEMPTS);
        redisTemplate.opsForValue().set(availableKey, Integer.toString(INITIAL_STOCK));
        redisTemplate.opsForValue().set(lockedKey, "0");
        redisTemplate.opsForValue().set(soldKey, "0");

        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        CountDownLatch ready = new CountDownLatch(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>(ATTEMPTS);

        try {
            for (int index = 0; index < ATTEMPTS; index++) {
                String reservationId = "concurrency-test-" + index;
                reservationKeys.add(redisStockService.reservationKey(reservationId));
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        redisStockService.reserveStock(TEST_SKU_ID, 1, reservationId);
                        succeeded.incrementAndGet();
                    } catch (BusinessException exception) {
                        if (exception.errorCode() == ErrorCode.TRADE_STOCK_NOT_ENOUGH) {
                            rejected.incrementAndGet();
                            return;
                        }
                        throw exception;
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }

            assertThat(succeeded).hasValue(INITIAL_STOCK);
            assertThat(rejected).hasValue(ATTEMPTS - INITIAL_STOCK);
            assertThat(redisTemplate.opsForValue().get(availableKey)).isEqualTo("0");
            assertThat(redisTemplate.opsForValue().get(lockedKey)).isEqualTo(Integer.toString(INITIAL_STOCK));
            assertThat(redisTemplate.opsForValue().get(soldKey)).isEqualTo("0");
        } finally {
            start.countDown();
            executor.shutdownNow();
            redisTemplate.delete(List.of(availableKey, lockedKey, soldKey));
            redisTemplate.delete(reservationKeys);
        }
    }
}
