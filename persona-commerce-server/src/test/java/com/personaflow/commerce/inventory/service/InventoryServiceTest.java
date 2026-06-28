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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryStockMapper inventoryStockMapper;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryStockMapper);
    }

    @Test
    void lockStockSucceedsWhenConditionalUpdateAffectsOneRow() {
        when(inventoryStockMapper.lockStock(30001L, 2)).thenReturn(1);

        inventoryService.lockStock(30001L, 2);

        verify(inventoryStockMapper).lockStock(30001L, 2);
        verify(inventoryStockMapper, never()).selectBySkuId(30001L);
    }

    @Test
    void lockStockRejectsInvalidQuantity() {
        assertBusinessError(
                () -> inventoryService.lockStock(30001L, 0),
                ErrorCode.TRADE_INVALID_QUANTITY
        );
        verifyNoInteractions(inventoryStockMapper);
    }

    @Test
    void lockStockRejectsMissingStockRecord() {
        when(inventoryStockMapper.lockStock(30001L, 2)).thenReturn(0);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(null);

        assertBusinessError(
                () -> inventoryService.lockStock(30001L, 2),
                ErrorCode.TRADE_STOCK_NOT_FOUND
        );
    }

    @Test
    void lockStockRejectsInsufficientAvailableStock() {
        when(inventoryStockMapper.lockStock(30001L, 2)).thenReturn(0);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(stock());

        assertBusinessError(
                () -> inventoryService.lockStock(30001L, 2),
                ErrorCode.TRADE_STOCK_NOT_ENOUGH
        );
    }

    @Test
    void releaseLockedStockSucceedsWhenConditionalUpdateAffectsOneRow() {
        when(inventoryStockMapper.releaseLockedStock(30001L, 2)).thenReturn(1);

        inventoryService.releaseLockedStock(30001L, 2);

        verify(inventoryStockMapper).releaseLockedStock(30001L, 2);
        verify(inventoryStockMapper, never()).selectBySkuId(30001L);
    }

    @Test
    void releaseLockedStockRejectsMissingStockRecord() {
        when(inventoryStockMapper.releaseLockedStock(30001L, 2)).thenReturn(0);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(null);

        assertBusinessError(
                () -> inventoryService.releaseLockedStock(30001L, 2),
                ErrorCode.TRADE_STOCK_NOT_FOUND
        );
    }

    @Test
    void releaseLockedStockRejectsInvalidStockState() {
        when(inventoryStockMapper.releaseLockedStock(30001L, 2)).thenReturn(0);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(stock());

        assertBusinessError(
                () -> inventoryService.releaseLockedStock(30001L, 2),
                ErrorCode.TRADE_STOCK_STATE_INVALID
        );
    }

    @Test
    void confirmLockedStockSucceedsWhenConditionalUpdateAffectsOneRow() {
        when(inventoryStockMapper.confirmLockedStock(30001L, 2)).thenReturn(1);

        inventoryService.confirmLockedStock(30001L, 2);

        verify(inventoryStockMapper).confirmLockedStock(30001L, 2);
        verify(inventoryStockMapper, never()).selectBySkuId(30001L);
    }

    @Test
    void confirmLockedStockRejectsMissingStockRecord() {
        when(inventoryStockMapper.confirmLockedStock(30001L, 2)).thenReturn(0);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(null);

        assertBusinessError(
                () -> inventoryService.confirmLockedStock(30001L, 2),
                ErrorCode.TRADE_STOCK_NOT_FOUND
        );
    }

    @Test
    void confirmLockedStockRejectsInvalidStockState() {
        when(inventoryStockMapper.confirmLockedStock(30001L, 2)).thenReturn(0);
        when(inventoryStockMapper.selectBySkuId(30001L)).thenReturn(stock());

        assertBusinessError(
                () -> inventoryService.confirmLockedStock(30001L, 2),
                ErrorCode.TRADE_STOCK_STATE_INVALID
        );
    }

    private InventoryStockEntity stock() {
        InventoryStockEntity stock = new InventoryStockEntity();
        stock.setId(1L);
        stock.setSkuId(30001L);
        stock.setAvailableQuantity(1);
        stock.setLockedQuantity(1);
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
