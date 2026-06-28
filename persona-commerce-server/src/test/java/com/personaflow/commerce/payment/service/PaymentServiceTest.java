package com.personaflow.commerce.payment.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.service.InventoryService;
import com.personaflow.commerce.order.entity.TradeOrderEntity;
import com.personaflow.commerce.order.entity.TradeOrderItemEntity;
import com.personaflow.commerce.order.mapper.TradeOrderItemMapper;
import com.personaflow.commerce.order.mapper.TradeOrderMapper;
import com.personaflow.commerce.order.service.OrderService;
import com.personaflow.commerce.payment.dto.PayOrderRequest;
import com.personaflow.commerce.payment.entity.PaymentRecordEntity;
import com.personaflow.commerce.payment.mapper.PaymentRecordMapper;
import com.personaflow.commerce.payment.support.PaymentNoGenerator;
import com.personaflow.commerce.payment.vo.PaymentVO;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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
class PaymentServiceTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentNoGenerator paymentNoGenerator;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                tradeOrderMapper,
                tradeOrderItemMapper,
                paymentRecordMapper,
                currentUserProvider,
                inventoryService,
                paymentNoGenerator
        );
    }

    @Test
    void payPendingOrderSucceedsWritesPaymentRecordAndConfirmsLockedStock() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(
                orderItem(60001L, 30001L, 2),
                orderItem(60002L, 30002L, 1)
        ));
        when(tradeOrderMapper.markOrderPaid(
                eq(50001L),
                eq(10001L),
                eq(OrderService.STATUS_PENDING_PAYMENT),
                eq(OrderService.STATUS_PAID),
                any(LocalDateTime.class)
        )).thenReturn(1);
        when(paymentNoGenerator.generate()).thenReturn("PAY20260629093000123456");
        when(paymentRecordMapper.insert(any(PaymentRecordEntity.class))).thenAnswer(invocation -> {
            PaymentRecordEntity paymentRecord = invocation.getArgument(0);
            paymentRecord.setId(70001L);
            return 1;
        });

        PaymentVO result = paymentService.payOrder(50001L, new PayOrderRequest("MOCK"));

        assertThat(result.paymentNo()).isEqualTo("PAY20260629093000123456");
        assertThat(result.orderId()).isEqualTo(50001L);
        assertThat(result.orderNo()).isEqualTo("PF20260628230000000123");
        assertThat(result.amount()).isEqualByComparingTo("918.00");
        assertThat(result.channel()).isEqualTo("MOCK");
        assertThat(result.status()).isEqualTo(PaymentService.PAYMENT_STATUS_SUCCESS);
        assertThat(result.paidAt()).isNotNull();

        verify(tradeOrderMapper).markOrderPaid(
                eq(50001L),
                eq(10001L),
                eq(OrderService.STATUS_PENDING_PAYMENT),
                eq(OrderService.STATUS_PAID),
                any(LocalDateTime.class)
        );
        verify(inventoryService).confirmLockedStock(30001L, 2);
        verify(inventoryService).confirmLockedStock(30002L, 1);

        ArgumentCaptor<PaymentRecordEntity> paymentRecordCaptor = ArgumentCaptor.forClass(PaymentRecordEntity.class);
        verify(paymentRecordMapper).insert(paymentRecordCaptor.capture());
        PaymentRecordEntity paymentRecord = paymentRecordCaptor.getValue();
        assertThat(paymentRecord.getPaymentNo()).isEqualTo("PAY20260629093000123456");
        assertThat(paymentRecord.getOrderId()).isEqualTo(50001L);
        assertThat(paymentRecord.getOrderNo()).isEqualTo("PF20260628230000000123");
        assertThat(paymentRecord.getUserId()).isEqualTo(10001L);
        assertThat(paymentRecord.getAmount()).isEqualByComparingTo("918.00");
        assertThat(paymentRecord.getChannel()).isEqualTo("MOCK");
        assertThat(paymentRecord.getStatus()).isEqualTo(PaymentService.PAYMENT_STATUS_SUCCESS);
        assertThat(paymentRecord.getPaidAt()).isNotNull();
        assertThat(paymentRecord.getCreatedAt()).isEqualTo(paymentRecord.getPaidAt());
        assertThat(paymentRecord.getUpdatedAt()).isEqualTo(paymentRecord.getPaidAt());
    }

    @Test
    void payOrderDefaultsMissingChannelToMock() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(60001L, 30001L, 2)));
        when(tradeOrderMapper.markOrderPaid(any(), any(), any(), any(), any())).thenReturn(1);
        when(paymentNoGenerator.generate()).thenReturn("PAY20260629093000123456");
        when(paymentRecordMapper.insert(any(PaymentRecordEntity.class))).thenReturn(1);

        PaymentVO result = paymentService.payOrder(50001L, null);

        assertThat(result.channel()).isEqualTo("MOCK");
        ArgumentCaptor<PaymentRecordEntity> paymentRecordCaptor = ArgumentCaptor.forClass(PaymentRecordEntity.class);
        verify(paymentRecordMapper).insert(paymentRecordCaptor.capture());
        assertThat(paymentRecordCaptor.getValue().getChannel()).isEqualTo("MOCK");
    }

    @Test
    void payOrderRejectsUnsupportedChannel() {
        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("WECHAT")),
                ErrorCode.COMMON_BAD_REQUEST
        );
        verifyNoInteractions(currentUserProvider, tradeOrderMapper, tradeOrderItemMapper, paymentRecordMapper, inventoryService);
    }

    @Test
    void payMissingOrderReturnsNotFound() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> paymentService.payOrder(99999L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_ORDER_NOT_FOUND
        );
        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(tradeOrderMapper, never()).markOrderPaid(any(), any(), any(), any(), any());
        verifyNoInteractions(paymentRecordMapper, inventoryService, paymentNoGenerator);
    }

    @Test
    void payForeignOrderReturnsNotFound() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_ORDER_NOT_FOUND
        );
        verifyNoInteractions(paymentRecordMapper, inventoryService, paymentNoGenerator);
    }

    @Test
    void payPaidOrderReturnsStatusNotAllowed() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PAID));

        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED
        );
        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(tradeOrderMapper, never()).markOrderPaid(any(), any(), any(), any(), any());
        verifyNoInteractions(paymentRecordMapper, inventoryService, paymentNoGenerator);
    }

    @Test
    void payCanceledOrderReturnsStatusNotAllowed() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_CANCELED));

        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED
        );
        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(tradeOrderMapper, never()).markOrderPaid(any(), any(), any(), any(), any());
        verifyNoInteractions(paymentRecordMapper, inventoryService, paymentNoGenerator);
    }

    @Test
    void payReturnsStatusNotAllowedWhenConditionalUpdateFails() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(60001L, 30001L, 2)));
        when(tradeOrderMapper.markOrderPaid(
                eq(50001L),
                eq(10001L),
                eq(OrderService.STATUS_PENDING_PAYMENT),
                eq(OrderService.STATUS_PAID),
                any(LocalDateTime.class)
        )).thenReturn(0);

        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED
        );
        verify(paymentRecordMapper, never()).insert(any(PaymentRecordEntity.class));
        verifyNoInteractions(inventoryService, paymentNoGenerator);
    }

    @Test
    void payDuplicatePaymentRecordReturnsPaymentRecordExists() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(60001L, 30001L, 2)));
        when(tradeOrderMapper.markOrderPaid(any(), any(), any(), any(), any())).thenReturn(1);
        when(paymentNoGenerator.generate()).thenReturn("PAY20260629093000123456");
        when(paymentRecordMapper.insert(any(PaymentRecordEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate payment"));

        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_PAYMENT_RECORD_EXISTS
        );
        verifyNoInteractions(inventoryService);
    }

    @Test
    void payPropagatesInventoryConfirmFailure() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(OrderService.STATUS_PENDING_PAYMENT));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(60001L, 30001L, 2)));
        when(tradeOrderMapper.markOrderPaid(any(), any(), any(), any(), any())).thenReturn(1);
        when(paymentNoGenerator.generate()).thenReturn("PAY20260629093000123456");
        when(paymentRecordMapper.insert(any(PaymentRecordEntity.class))).thenReturn(1);
        doThrow(new BusinessException(ErrorCode.TRADE_STOCK_STATE_INVALID))
                .when(inventoryService)
                .confirmLockedStock(30001L, 2);

        assertBusinessError(
                () -> paymentService.payOrder(50001L, new PayOrderRequest("MOCK")),
                ErrorCode.TRADE_STOCK_STATE_INVALID
        );
        verify(paymentRecordMapper).insert(any(PaymentRecordEntity.class));
        verify(inventoryService).confirmLockedStock(30001L, 2);
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
        orderItem.setCategoryName("Keyboard Mouse");
        orderItem.setProductName("KeyForge K3");
        orderItem.setSkuName("White");
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
