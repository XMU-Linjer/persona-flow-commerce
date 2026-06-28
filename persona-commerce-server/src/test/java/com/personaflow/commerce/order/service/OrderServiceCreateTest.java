package com.personaflow.commerce.order.service;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.service.InventoryService;
import com.personaflow.commerce.order.dto.CreateOrderItemRequest;
import com.personaflow.commerce.order.dto.CreateOrderRequest;
import com.personaflow.commerce.order.entity.TradeOrderEntity;
import com.personaflow.commerce.order.entity.TradeOrderItemEntity;
import com.personaflow.commerce.order.mapper.TradeOrderItemMapper;
import com.personaflow.commerce.order.mapper.TradeOrderMapper;
import com.personaflow.commerce.order.support.OrderNoGenerator;
import com.personaflow.commerce.order.vo.OrderCreateVO;
import com.personaflow.commerce.product.api.ProductQueryApi;
import com.personaflow.commerce.product.api.model.ProductSnapshot;
import com.personaflow.commerce.user.api.AddressQueryApi;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.AddressSnapshot;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceCreateTest {

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;

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
                currentUserProvider,
                addressQueryApi,
                productQueryApi,
                inventoryService,
                orderNoGenerator
        );
    }

    @Test
    void createOrderSucceedsWithAddressSnapshotProductSnapshotAndLockedStock() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressQueryApi.requireOwnedAddress(10001L, 1L)).thenReturn(addressSnapshot());
        when(productQueryApi.requireSellableSkus(List.of(30001L, 30002L))).thenReturn(productSnapshots());
        when(orderNoGenerator.generate()).thenReturn("PF20260628230000000123");
        when(tradeOrderMapper.insert(any(TradeOrderEntity.class))).thenAnswer(invocation -> {
            TradeOrderEntity order = invocation.getArgument(0);
            order.setId(50001L);
            return 1;
        });
        when(tradeOrderItemMapper.insert(any(TradeOrderItemEntity.class))).thenReturn(1);

        OrderCreateVO result = orderService.createOrder(request());

        assertThat(result.orderId()).isEqualTo(50001L);
        assertThat(result.orderNo()).isEqualTo("PF20260628230000000123");
        assertThat(result.status()).isEqualTo(OrderService.STATUS_PENDING_PAYMENT);
        assertThat(result.totalAmount()).isEqualByComparingTo("1047.00");
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).productName()).isEqualTo("KeyForge K3");
        assertThat(result.items().get(0).unitPrice()).isEqualByComparingTo("459.00");
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.items().get(0).subtotal()).isEqualByComparingTo("918.00");

        verify(currentUserProvider).requireCurrentUser();
        verify(addressQueryApi).requireOwnedAddress(10001L, 1L);
        verify(productQueryApi).requireSellableSkus(List.of(30001L, 30002L));
        verify(inventoryService).lockStock(30001L, 2);
        verify(inventoryService).lockStock(30002L, 1);

        ArgumentCaptor<TradeOrderEntity> orderCaptor = ArgumentCaptor.forClass(TradeOrderEntity.class);
        verify(tradeOrderMapper).insert(orderCaptor.capture());
        TradeOrderEntity savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getOrderNo()).isEqualTo("PF20260628230000000123");
        assertThat(savedOrder.getUserId()).isEqualTo(10001L);
        assertThat(savedOrder.getAddressId()).isEqualTo(1L);
        assertThat(savedOrder.getRecipientName()).isEqualTo("Ada");
        assertThat(savedOrder.getRecipientPhone()).isEqualTo("13800000000");
        assertThat(savedOrder.getProvince()).isEqualTo("Zhejiang");
        assertThat(savedOrder.getCity()).isEqualTo("Hangzhou");
        assertThat(savedOrder.getDistrict()).isEqualTo("Xihu");
        assertThat(savedOrder.getDetailAddress()).isEqualTo("No. 1 West Lake Road");
        assertThat(savedOrder.getPostalCode()).isEqualTo("310000");
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("1047.00");
        assertThat(savedOrder.getStatus()).isEqualTo(OrderService.STATUS_PENDING_PAYMENT);
        assertThat(savedOrder.getPaidAt()).isNull();
        assertThat(savedOrder.getCanceledAt()).isNull();

        ArgumentCaptor<TradeOrderItemEntity> itemCaptor = ArgumentCaptor.forClass(TradeOrderItemEntity.class);
        verify(tradeOrderItemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        TradeOrderItemEntity firstItem = itemCaptor.getAllValues().get(0);
        assertThat(firstItem.getOrderId()).isEqualTo(50001L);
        assertThat(firstItem.getOrderNo()).isEqualTo("PF20260628230000000123");
        assertThat(firstItem.getUserId()).isEqualTo(10001L);
        assertThat(firstItem.getSkuId()).isEqualTo(30001L);
        assertThat(firstItem.getSpuId()).isEqualTo(20001L);
        assertThat(firstItem.getCategoryId()).isEqualTo(201L);
        assertThat(firstItem.getCategoryName()).isEqualTo("键盘鼠标");
        assertThat(firstItem.getProductName()).isEqualTo("KeyForge K3");
        assertThat(firstItem.getSkuName()).isEqualTo("青轴 白色");
        assertThat(firstItem.getImageUrl()).isEqualTo("sku-30001.jpg");
        assertThat(firstItem.getUnitPrice()).isEqualByComparingTo("459.00");
        assertThat(firstItem.getQuantity()).isEqualTo(2);
        assertThat(firstItem.getSubtotal()).isEqualByComparingTo("918.00");
    }

    @Test
    void createOrderRejectsEmptyItems() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());

        assertBusinessError(
                () -> orderService.createOrder(new CreateOrderRequest(1L, List.of())),
                ErrorCode.TRADE_ORDER_EMPTY_ITEMS
        );
        verifyNoInteractions(addressQueryApi, productQueryApi, inventoryService);
        verify(tradeOrderMapper, never()).insert(any(TradeOrderEntity.class));
        verify(tradeOrderItemMapper, never()).insert(any(TradeOrderItemEntity.class));
    }

    @Test
    void createOrderRejectsInvalidQuantity() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());

        assertBusinessError(
                () -> orderService.createOrder(new CreateOrderRequest(
                        1L,
                        List.of(new CreateOrderItemRequest(30001L, 0))
                )),
                ErrorCode.TRADE_INVALID_QUANTITY
        );
        verifyNoInteractions(addressQueryApi, productQueryApi, inventoryService);
        verify(tradeOrderMapper, never()).insert(any(TradeOrderEntity.class));
        verify(tradeOrderItemMapper, never()).insert(any(TradeOrderItemEntity.class));
    }

    @Test
    void createOrderRejectsDuplicateSku() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());

        assertBusinessError(
                () -> orderService.createOrder(new CreateOrderRequest(
                        1L,
                        List.of(
                                new CreateOrderItemRequest(30001L, 1),
                                new CreateOrderItemRequest(30001L, 2)
                        )
                )),
                ErrorCode.TRADE_DUPLICATE_SKU
        );
        verifyNoInteractions(addressQueryApi, productQueryApi, inventoryService);
        verify(tradeOrderMapper, never()).insert(any(TradeOrderEntity.class));
        verify(tradeOrderItemMapper, never()).insert(any(TradeOrderItemEntity.class));
    }

    @Test
    void createOrderDoesNotWriteOrderWhenProductQueryFails() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressQueryApi.requireOwnedAddress(10001L, 1L)).thenReturn(addressSnapshot());
        when(productQueryApi.requireSellableSkus(List.of(30001L, 30002L)))
                .thenThrow(new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE));

        assertBusinessError(
                () -> orderService.createOrder(request()),
                ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE
        );
        verifyNoInteractions(inventoryService);
        verify(tradeOrderMapper, never()).insert(any(TradeOrderEntity.class));
        verify(tradeOrderItemMapper, never()).insert(any(TradeOrderItemEntity.class));
    }

    @Test
    void createOrderDoesNotWriteOrderItemWhenInventoryLockFails() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(addressQueryApi.requireOwnedAddress(10001L, 1L)).thenReturn(addressSnapshot());
        when(productQueryApi.requireSellableSkus(List.of(30001L, 30002L))).thenReturn(productSnapshots());
        doThrow(new BusinessException(ErrorCode.TRADE_STOCK_NOT_ENOUGH))
                .when(inventoryService)
                .lockStock(30001L, 2);

        assertBusinessError(
                () -> orderService.createOrder(request()),
                ErrorCode.TRADE_STOCK_NOT_ENOUGH
        );
        verify(tradeOrderMapper, never()).insert(any(TradeOrderEntity.class));
        verify(tradeOrderItemMapper, never()).insert(any(TradeOrderItemEntity.class));
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(
                1L,
                List.of(
                        new CreateOrderItemRequest(30001L, 2),
                        new CreateOrderItemRequest(30002L, 1)
                )
        );
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private AddressSnapshot addressSnapshot() {
        return new AddressSnapshot(
                1L,
                "Ada",
                "13800000000",
                "Zhejiang",
                "Hangzhou",
                "Xihu",
                "No. 1 West Lake Road",
                "310000"
        );
    }

    private Map<Long, ProductSnapshot> productSnapshots() {
        Map<Long, ProductSnapshot> snapshots = new LinkedHashMap<>();
        snapshots.put(30001L, new ProductSnapshot(
                30001L,
                20001L,
                201L,
                "键盘鼠标",
                "KeyForge K3",
                "青轴 白色",
                new BigDecimal("459.00"),
                "sku-30001.jpg"
        ));
        snapshots.put(30002L, new ProductSnapshot(
                30002L,
                20002L,
                201L,
                "键盘鼠标",
                "SilentPro M8",
                "白色",
                new BigDecimal("129.00"),
                "sku-30002.jpg"
        ));
        return snapshots;
    }

    private void assertBusinessError(Runnable operation, ErrorCode expectedErrorCode) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(expectedErrorCode);
    }
}
