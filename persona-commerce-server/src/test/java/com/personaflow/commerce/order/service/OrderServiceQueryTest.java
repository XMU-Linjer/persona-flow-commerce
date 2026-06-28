package com.personaflow.commerce.order.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.inventory.service.InventoryService;
import com.personaflow.commerce.order.entity.TradeOrderEntity;
import com.personaflow.commerce.order.entity.TradeOrderItemEntity;
import com.personaflow.commerce.order.mapper.TradeOrderItemMapper;
import com.personaflow.commerce.order.mapper.TradeOrderMapper;
import com.personaflow.commerce.order.support.OrderNoGenerator;
import com.personaflow.commerce.order.vo.OrderDetailVO;
import com.personaflow.commerce.order.vo.OrderListItemVO;
import com.personaflow.commerce.payment.entity.PaymentRecordEntity;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceQueryTest {

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
    void listOrdersReturnsCurrentUserOrderPage() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(
                order(50002L, "PF20260628230200000124", 20),
                order(50001L, "PF20260628230000000123", 10)
        ));

        PageResult<OrderListItemVO> result = orderService.listOrders(null, 1, 10);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.records()).hasSize(2);
        assertThat(result.records().get(0).orderId()).isEqualTo(50002L);
        assertThat(result.records().get(0).orderNo()).isEqualTo("PF20260628230200000124");
        assertThat(result.records().get(0).status()).isEqualTo(20);
        assertThat(result.records().get(0).totalAmount()).isEqualByComparingTo("129.00");

        verify(currentUserProvider).requireCurrentUser();
        verify(tradeOrderMapper).selectList(any());
        verifyNoInteractions(tradeOrderItemMapper, paymentRecordMapper, addressQueryApi, productQueryApi, inventoryService);
    }

    @Test
    void listOrdersSupportsStatusFilter() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(order(50002L, "PF20260628230200000124", 20)));

        PageResult<OrderListItemVO> result = orderService.listOrders(20, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).status()).isEqualTo(20);
        verify(tradeOrderMapper).selectList(any());
        verifyNoInteractions(tradeOrderItemMapper, paymentRecordMapper, addressQueryApi, productQueryApi, inventoryService);
    }

    @Test
    void listOrdersReturnsEmptyPage() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of());

        PageResult<OrderListItemVO> result = orderService.listOrders(null, 2, 5);

        assertThat(result.records()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.total()).isZero();
        verifyNoInteractions(tradeOrderItemMapper, paymentRecordMapper, addressQueryApi, productQueryApi, inventoryService);
    }

    @Test
    void getOrderDetailReturnsAddressSnapshotItemsAndNullPayment() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(50001L, "PF20260628230000000123", 10));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem()));
        when(paymentRecordMapper.selectOne(any())).thenReturn(null);

        OrderDetailVO result = orderService.getOrderDetail(50001L);

        assertThat(result.orderId()).isEqualTo(50001L);
        assertThat(result.userId()).isEqualTo(10001L);
        assertThat(result.address().addressId()).isEqualTo(1L);
        assertThat(result.address().recipientName()).isEqualTo("Ada");
        assertThat(result.address().recipientPhone()).isEqualTo("13800000000");
        assertThat(result.address().detailAddress()).isEqualTo("No. 1 West Lake Road");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).skuId()).isEqualTo(30001L);
        assertThat(result.items().get(0).productName()).isEqualTo("KeyForge K3");
        assertThat(result.items().get(0).skuName()).isEqualTo("青轴 白色");
        assertThat(result.items().get(0).subtotal()).isEqualByComparingTo("918.00");
        assertThat(result.payment()).isNull();

        verifyNoInteractions(addressQueryApi, productQueryApi, inventoryService);
    }

    @Test
    void getOrderDetailCanReturnPaymentRecord() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(order(50001L, "PF20260628230000000123", 20));
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem()));
        when(paymentRecordMapper.selectOne(any())).thenReturn(paymentRecord());

        OrderDetailVO result = orderService.getOrderDetail(50001L);

        assertThat(result.payment()).isNotNull();
        assertThat(result.payment().paymentNo()).isEqualTo("PAY202606282310000001");
        assertThat(result.payment().amount()).isEqualByComparingTo("918.00");
        assertThat(result.payment().channel()).isEqualTo("MOCK");
        assertThat(result.payment().status()).isEqualTo(20);
        verifyNoInteractions(addressQueryApi, productQueryApi, inventoryService);
    }

    @Test
    void getOrderDetailRejectsMissingOrForeignOrder() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(tradeOrderMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> orderService.getOrderDetail(99999L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ErrorCode.TRADE_ORDER_NOT_FOUND);

        verify(tradeOrderItemMapper, never()).selectList(any());
        verify(paymentRecordMapper, never()).selectOne(any());
        verifyNoInteractions(addressQueryApi, productQueryApi, inventoryService);
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private TradeOrderEntity order(Long orderId, String orderNo, Integer status) {
        TradeOrderEntity order = new TradeOrderEntity();
        order.setId(orderId);
        order.setOrderNo(orderNo);
        order.setUserId(10001L);
        order.setAddressId(1L);
        order.setRecipientName("Ada");
        order.setRecipientPhone("13800000000");
        order.setProvince("Zhejiang");
        order.setCity("Hangzhou");
        order.setDistrict("Xihu");
        order.setDetailAddress("No. 1 West Lake Road");
        order.setPostalCode("310000");
        order.setTotalAmount(new BigDecimal(status == 20 ? "129.00" : "918.00"));
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.of(2026, 6, 28, 23, orderId.intValue() % 60));
        order.setPaidAt(status == 20 ? LocalDateTime.of(2026, 6, 28, 23, 30) : null);
        order.setCanceledAt(status == 30 ? LocalDateTime.of(2026, 6, 28, 23, 40) : null);
        return order;
    }

    private TradeOrderItemEntity orderItem() {
        TradeOrderItemEntity orderItem = new TradeOrderItemEntity();
        orderItem.setId(60001L);
        orderItem.setOrderId(50001L);
        orderItem.setOrderNo("PF20260628230000000123");
        orderItem.setUserId(10001L);
        orderItem.setSkuId(30001L);
        orderItem.setSpuId(20001L);
        orderItem.setCategoryId(201L);
        orderItem.setCategoryName("键盘鼠标");
        orderItem.setProductName("KeyForge K3");
        orderItem.setSkuName("青轴 白色");
        orderItem.setImageUrl("sku-30001.jpg");
        orderItem.setUnitPrice(new BigDecimal("459.00"));
        orderItem.setQuantity(2);
        orderItem.setSubtotal(new BigDecimal("918.00"));
        orderItem.setCreatedAt(LocalDateTime.of(2026, 6, 28, 23, 1));
        return orderItem;
    }

    private PaymentRecordEntity paymentRecord() {
        PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
        paymentRecord.setId(70001L);
        paymentRecord.setPaymentNo("PAY202606282310000001");
        paymentRecord.setOrderId(50001L);
        paymentRecord.setOrderNo("PF20260628230000000123");
        paymentRecord.setUserId(10001L);
        paymentRecord.setAmount(new BigDecimal("918.00"));
        paymentRecord.setChannel("MOCK");
        paymentRecord.setStatus(20);
        paymentRecord.setPaidAt(LocalDateTime.of(2026, 6, 28, 23, 30));
        return paymentRecord;
    }
}
