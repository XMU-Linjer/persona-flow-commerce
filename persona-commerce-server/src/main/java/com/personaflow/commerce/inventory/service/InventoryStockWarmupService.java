package com.personaflow.commerce.inventory.service;

import com.personaflow.commerce.inventory.entity.InventoryStockEntity;
import com.personaflow.commerce.inventory.mapper.InventoryStockMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryStockWarmupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryStockWarmupService.class);

    private final InventoryStockMapper inventoryStockMapper;
    private final RedisStockService redisStockService;

    public InventoryStockWarmupService(
            InventoryStockMapper inventoryStockMapper,
            RedisStockService redisStockService
    ) {
        this.inventoryStockMapper = inventoryStockMapper;
        this.redisStockService = redisStockService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        try {
            List<InventoryStockEntity> stocks = inventoryStockMapper.selectList(null);
            for (InventoryStockEntity stock : stocks) {
                redisStockService.synchronizeStock(stock);
            }
            LOGGER.info("Redis inventory stock synchronization completed: {} SKU records", stocks.size());
        } catch (RuntimeException exception) {
            LOGGER.warn("Redis inventory stock warmup failed; order creation will fail closed until Redis recovers");
        }
    }
}
