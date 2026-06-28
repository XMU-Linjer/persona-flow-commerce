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
import com.personaflow.commerce.order.vo.OrderItemVO;
import com.personaflow.commerce.product.api.ProductQueryApi;
import com.personaflow.commerce.product.api.model.ProductSnapshot;
import com.personaflow.commerce.user.api.AddressQueryApi;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.AddressSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    public static final int STATUS_PENDING_PAYMENT = 10;

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AddressQueryApi addressQueryApi;
    private final ProductQueryApi productQueryApi;
    private final InventoryService inventoryService;
    private final OrderNoGenerator orderNoGenerator;

    public OrderService(
            TradeOrderMapper tradeOrderMapper,
            TradeOrderItemMapper tradeOrderItemMapper,
            CurrentUserProvider currentUserProvider,
            AddressQueryApi addressQueryApi,
            ProductQueryApi productQueryApi,
            InventoryService inventoryService,
            OrderNoGenerator orderNoGenerator
    ) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.currentUserProvider = currentUserProvider;
        this.addressQueryApi = addressQueryApi;
        this.productQueryApi = productQueryApi;
        this.inventoryService = inventoryService;
        this.orderNoGenerator = orderNoGenerator;
    }

    @Transactional
    public OrderCreateVO createOrder(CreateOrderRequest request) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        validateItems(request.items());

        List<Long> skuIds = request.items()
                .stream()
                .map(CreateOrderItemRequest::skuId)
                .toList();
        AddressSnapshot addressSnapshot = addressQueryApi.requireOwnedAddress(userId, request.addressId());
        Map<Long, ProductSnapshot> productSnapshots = productQueryApi.requireSellableSkus(skuIds);

        List<PreparedOrderItem> preparedItems = request.items()
                .stream()
                .map(item -> prepareOrderItem(item, productSnapshots.get(item.skuId())))
                .toList();
        BigDecimal totalAmount = preparedItems.stream()
                .map(PreparedOrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        for (CreateOrderItemRequest item : request.items()) {
            inventoryService.lockStock(item.skuId(), item.quantity());
        }

        LocalDateTime now = LocalDateTime.now();
        TradeOrderEntity order = new TradeOrderEntity();
        order.setOrderNo(orderNoGenerator.generate());
        order.setUserId(userId);
        order.setAddressId(addressSnapshot.addressId());
        order.setRecipientName(addressSnapshot.recipientName());
        order.setRecipientPhone(addressSnapshot.recipientPhone());
        order.setProvince(addressSnapshot.province());
        order.setCity(addressSnapshot.city());
        order.setDistrict(addressSnapshot.district());
        order.setDetailAddress(addressSnapshot.detailAddress());
        order.setPostalCode(addressSnapshot.postalCode());
        order.setTotalAmount(totalAmount);
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        expectOneRow(tradeOrderMapper.insert(order), "Failed to create order");

        List<OrderItemVO> itemVOs = preparedItems.stream()
                .map(preparedItem -> {
                    TradeOrderItemEntity orderItem = toOrderItemEntity(
                            preparedItem,
                            order.getId(),
                            order.getOrderNo(),
                            userId,
                            now
                    );
                    expectOneRow(tradeOrderItemMapper.insert(orderItem), "Failed to create order item");
                    return toOrderItemVO(orderItem);
                })
                .toList();

        return new OrderCreateVO(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemVOs
        );
    }

    private void validateItems(List<CreateOrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_EMPTY_ITEMS);
        }

        Set<Long> skuIds = new LinkedHashSet<>();
        for (CreateOrderItemRequest item : items) {
            if (item == null || item.quantity() == null || item.quantity() <= 0) {
                throw new BusinessException(ErrorCode.TRADE_INVALID_QUANTITY);
            }
            if (!skuIds.add(item.skuId())) {
                throw new BusinessException(ErrorCode.TRADE_DUPLICATE_SKU);
            }
        }
    }

    private PreparedOrderItem prepareOrderItem(
            CreateOrderItemRequest item,
            ProductSnapshot productSnapshot
    ) {
        BigDecimal unitPrice = productSnapshot.unitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = unitPrice
                .multiply(BigDecimal.valueOf(item.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        return new PreparedOrderItem(productSnapshot, item.quantity(), subtotal);
    }

    private TradeOrderItemEntity toOrderItemEntity(
            PreparedOrderItem preparedItem,
            Long orderId,
            String orderNo,
            Long userId,
            LocalDateTime createdAt
    ) {
        ProductSnapshot snapshot = preparedItem.productSnapshot();
        TradeOrderItemEntity orderItem = new TradeOrderItemEntity();
        orderItem.setOrderId(orderId);
        orderItem.setOrderNo(orderNo);
        orderItem.setUserId(userId);
        orderItem.setSkuId(snapshot.skuId());
        orderItem.setSpuId(snapshot.spuId());
        orderItem.setCategoryId(snapshot.categoryId());
        orderItem.setCategoryName(snapshot.categoryName());
        orderItem.setProductName(snapshot.productName());
        orderItem.setSkuName(snapshot.skuName());
        orderItem.setImageUrl(snapshot.imageUrl());
        orderItem.setUnitPrice(snapshot.unitPrice().setScale(2, RoundingMode.HALF_UP));
        orderItem.setQuantity(preparedItem.quantity());
        orderItem.setSubtotal(preparedItem.subtotal());
        orderItem.setCreatedAt(createdAt);
        return orderItem;
    }

    private OrderItemVO toOrderItemVO(TradeOrderItemEntity orderItem) {
        return new OrderItemVO(
                orderItem.getSkuId(),
                orderItem.getSpuId(),
                orderItem.getCategoryId(),
                orderItem.getCategoryName(),
                orderItem.getProductName(),
                orderItem.getSkuName(),
                orderItem.getImageUrl(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity(),
                orderItem.getSubtotal()
        );
    }

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }

    private record PreparedOrderItem(
            ProductSnapshot productSnapshot,
            Integer quantity,
            BigDecimal subtotal
    ) {
    }
}
