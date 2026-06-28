package com.personaflow.commerce.inventory.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.entity.InventoryStockEntity;
import com.personaflow.commerce.inventory.mapper.InventoryStockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryStockMapper inventoryStockMapper;

    public InventoryService(InventoryStockMapper inventoryStockMapper) {
        this.inventoryStockMapper = inventoryStockMapper;
    }

    @Transactional
    public void lockStock(Long skuId, Integer quantity) {
        validateQuantity(quantity);

        int affectedRows = inventoryStockMapper.lockStock(skuId, quantity);
        if (affectedRows == 1) {
            return;
        }

        InventoryStockEntity stock = inventoryStockMapper.selectBySkuId(skuId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.TRADE_STOCK_NOT_FOUND);
        }
        throw new BusinessException(ErrorCode.TRADE_STOCK_NOT_ENOUGH);
    }

    @Transactional
    public void releaseLockedStock(Long skuId, Integer quantity) {
        validateQuantity(quantity);

        int affectedRows = inventoryStockMapper.releaseLockedStock(skuId, quantity);
        handleLockedStockChangeResult(skuId, affectedRows);
    }

    @Transactional
    public void confirmLockedStock(Long skuId, Integer quantity) {
        validateQuantity(quantity);

        int affectedRows = inventoryStockMapper.confirmLockedStock(skuId, quantity);
        handleLockedStockChangeResult(skuId, affectedRows);
    }

    private void handleLockedStockChangeResult(Long skuId, int affectedRows) {
        if (affectedRows == 1) {
            return;
        }

        InventoryStockEntity stock = inventoryStockMapper.selectBySkuId(skuId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.TRADE_STOCK_NOT_FOUND);
        }
        throw new BusinessException(ErrorCode.TRADE_STOCK_STATE_INVALID);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.TRADE_INVALID_QUANTITY);
        }
    }
}
