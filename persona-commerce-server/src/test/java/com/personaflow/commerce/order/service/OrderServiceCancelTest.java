package com.personaflow.commerce.order.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.service.InventoryService;
import com.personaflow.commerce.order.entity.TradeOrderEntity;
import com.personaflow.commerce.order.entity.TradeOrderItemEntity;
import com.personaflow.commerce.order.mapper.TradeOrderItemMapper;
import com.personaflow.commerce.order.mapper.TradeOrderMapper;
import com.personaflow.commerce.order.support.OrderNoGenerator;
import com.personaflow.commerce.order.vo.OrderStatusVO;
import com.personaflow.commerce.payment.mapper.PaymentRecordMapper;
import com.personaflow.commerce.product.api.ProductQueryApi;
import com.personaflow.commerce.user.api.AddressQueryApi;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCancelTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AddressQueryApi addressQueryApi;

    @Mock
    private ProductQueryApi productQueryApi;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderNoGenerator orderNoGenerator;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                tradeOrderMapper,
                tradeOrderItemMapper,
                paymentRecordMapper,
                currentUserProvider,
                addressQueryApi,
                productQueryApi,
                inventoryService,
                orderNoGenerator
        );
    }

    @Test
    void cancelPendingOrderSucceedsAndReleasesLockedStock() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(
                orderItem(60001L, 30001L, 2),
                orderItem(60002L, 30002L, 1)
        ));
        when(tradeOrderMapper.cancelPendingOrder(
                eq(50001L),
                eq(10001L),
                eq(OrderService.STATUS_PENDING_PAYMENT),
                eq(OrderService.STATUS_CANCELED),
                any(LocalDateTime.class)
        )).thenReturn(1);

        OrderStatusVO result = orderService.cancelOrder(50001L);

        assertThat(result.orderId()).isEqualTo(50001L);
        assertThat(result.orderNo()).isEqualTo("PF20260628230000000123");
        assertThat(result.status()).isEqualTo(OrderService.STATUS_CANCELED);
        assertThat(result.canceledAt()).isNotNull();
        verify(inventoryService).releaseLockedStock(30001L, 2);
        verify(inventoryService).releaseLockedStock(30002L, 1);
        verifyNoInteractions(addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    @Test
    void cancelMissingOrderReturnsNotFound() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> orderService.cancelOrder(99999L),
                ErrorCode.TRADE_ORDER_NOT_FOUND
        );
        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(tradeOrderMapper, never()).cancelPendingOrder(any(), any(), any(), any(), any());
        verifyNoInteractions(inventoryService, addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    @Test
    void cancelForeignOrderReturnsNotFound() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> orderService.cancelOrder(50001L),
                ErrorCode.TRADE_ORDER_NOT_FOUND
        );
        verifyNoInteractions(inventoryService, addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    @Test
    void cancelPaidOrderReturnsStatusNotAllowed() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(20));

        assertBusinessError(
                () -> orderService.cancelOrder(50001L),
                ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED
        );
        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(tradeOrderMapper, never()).cancelPendingOrder(any(), any(), any(), any(), any());
        verifyNoInteractions(inventoryService, addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    @Test
    void cancelCanceledOrderReturnsStatusNotAllowed() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_CANCELED));

        assertBusinessError(
                () -> orderService.cancelOrder(50001L),
                ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED
        );
        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(tradeOrderMapper, never()).cancelPendingOrder(any(), any(), any(), any(), any());
        verifyNoInteractions(inventoryService, addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    @Test
    void cancelReturnsStatusNotAllowedWhenConditionalUpdateFails() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(60001L, 30001L, 2)));
        when(tradeOrderMapper.cancelPendingOrder(
                eq(50001L),
                eq(10001L),
                eq(OrderService.STATUS_PENDING_PAYMENT),
                eq(OrderService.STATUS_CANCELED),
                any(LocalDateTime.class)
        )).thenReturn(0);

        assertBusinessError(
                () -> orderService.cancelOrder(50001L),
                ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED
        );
        verifyNoInteractions(inventoryService, addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    @Test
    void cancelPropagatesInventoryReleaseFailure() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(60001L, 30001L, 2)));
        when(tradeOrderMapper.cancelPendingOrder(
                eq(50001L),
                eq(10001L),
                eq(OrderService.STATUS_PENDING_PAYMENT),
                eq(OrderService.STATUS_CANCELED),
                any(LocalDateTime.class)
        )).thenReturn(1);
        doThrow(new BusinessException(ErrorCode.TRADE_STOCK_STATE_INVALID))
                .when(inventoryService)
                .releaseLockedStock(30001L, 2);

        assertBusinessError(
                () -> orderService.cancelOrder(50001L),
                ErrorCode.TRADE_STOCK_STATE_INVALID
        );
        verify(inventoryService).releaseLockedStock(30001L, 2);
        verifyNoInteractions(addressQueryApi, productQueryApi, paymentRecordMapper);
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private TradeOrderEntity order(Integer status) {
        TradeOrderEntity order = new TradeOrderEntity();
        order.setId(50001L);
        order.setOrderNo("PF20260628230000000123");
        order.setUserId(10001L);
        order.setAddressId(1L);
        order.setRecipientName("Ada");
        order.setRecipientPhone("13800000000");
        order.setProvince("Zhejiang");
        order.setCity("Hangzhou");
        order.setDistrict("Xihu");
        order.setDetailAddress("No. 1 West Lake Road");
        order.setPostalCode("310000");
        order.setTotalAmount(new BigDecimal("918.00"));
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.of(2026, 6, 28, 23, 0));
        return order;
    }

    private TradeOrderItemEntity orderItem(Long id, Long skuId, Integer quantity) {
        TradeOrderItemEntity orderItem = new TradeOrderItemEntity();
        orderItem.setId(id);
        orderItem.setOrderId(50001L);
        orderItem.setOrderNo("PF20260628230000000123");
        orderItem.setUserId(10001L);
        orderItem.setSkuId(skuId);
        orderItem.setSpuId(20001L);
        orderItem.setCategoryId(201L);
        orderItem.setCategoryName("键盘鼠标");
        orderItem.setProductName("KeyForge K3");
        orderItem.setSkuName("青轴 白色");
        orderItem.setImageUrl("sku-30001.jpg");
        orderItem.setUnitPrice(new BigDecimal("459.00"));
        orderItem.setQuantity(quantity);
        orderItem.setSubtotal(new BigDecimal("918.00"));
        orderItem.setCreatedAt(LocalDateTime.of(2026, 6, 28, 23, 1));
        return orderItem;
    }

    private void assertBusinessError(Runnable operation, ErrorCode expectedErrorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(expectedErrorCode);
    }
}
