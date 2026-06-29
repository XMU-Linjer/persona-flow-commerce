package com.personaflow.commerce.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishCommand;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishSupport;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.inventory.service.InventoryService;
import com.personaflow.commerce.order.dto.CreateOrderItemRequest;
import com.personaflow.commerce.order.dto.CreateOrderRequest;
import com.personaflow.commerce.order.entity.TradeOrderEntity;
import com.personaflow.commerce.order.entity.TradeOrderItemEntity;
import com.personaflow.commerce.order.mapper.TradeOrderItemMapper;
import com.personaflow.commerce.order.mapper.TradeOrderMapper;
import com.personaflow.commerce.order.support.OrderNoGenerator;
import com.personaflow.commerce.order.vo.OrderCreateVO;
import com.personaflow.commerce.order.vo.OrderAddressVO;
import com.personaflow.commerce.order.vo.OrderDetailVO;
import com.personaflow.commerce.order.vo.OrderItemVO;
import com.personaflow.commerce.order.vo.OrderListItemVO;
import com.personaflow.commerce.order.vo.OrderStatusVO;
import com.personaflow.commerce.payment.entity.PaymentRecordEntity;
import com.personaflow.commerce.payment.mapper.PaymentRecordMapper;
import com.personaflow.commerce.payment.vo.PaymentVO;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrderService {

    public static final int STATUS_PENDING_PAYMENT = 10;
    public static final int STATUS_PAID = 20;
    public static final int STATUS_CANCELED = 30;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String SOURCE_MODULE = "trade";

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AddressQueryApi addressQueryApi;
    private final ProductQueryApi productQueryApi;
    private final InventoryService inventoryService;
    private final OrderNoGenerator orderNoGenerator;
    private final BehaviorEventPublishSupport behaviorEventPublishSupport;

    public OrderService(
            TradeOrderMapper tradeOrderMapper,
            TradeOrderItemMapper tradeOrderItemMapper,
            PaymentRecordMapper paymentRecordMapper,
            CurrentUserProvider currentUserProvider,
            AddressQueryApi addressQueryApi,
            ProductQueryApi productQueryApi,
            InventoryService inventoryService,
            OrderNoGenerator orderNoGenerator,
            BehaviorEventPublishSupport behaviorEventPublishSupport
    ) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.currentUserProvider = currentUserProvider;
        this.addressQueryApi = addressQueryApi;
        this.productQueryApi = productQueryApi;
        this.inventoryService = inventoryService;
        this.orderNoGenerator = orderNoGenerator;
        this.behaviorEventPublishSupport = behaviorEventPublishSupport;
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
        behaviorEventPublishSupport.publishAfterCommit(orderCreatedCommand(userId, order, itemVOs));

        return new OrderCreateVO(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                itemVOs
        );
    }

    @Transactional(readOnly = true)
    public PageResult<OrderListItemVO> listOrders(Integer status, Integer page, Integer size) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        List<TradeOrderEntity> orders = tradeOrderMapper.selectList(
                Wrappers.<TradeOrderEntity>lambdaQuery()
                        .eq(TradeOrderEntity::getUserId, userId)
                        .eq(status != null, TradeOrderEntity::getStatus, status)
                        .orderByDesc(TradeOrderEntity::getCreatedAt)
                        .orderByDesc(TradeOrderEntity::getId)
        );
        if (orders.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0);
        }

        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, orders.size());
        int toIndex = Math.min(fromIndex + normalizedSize, orders.size());
        return new PageResult<>(
                orders.subList(fromIndex, toIndex)
                        .stream()
                        .map(this::toOrderListItemVO)
                        .toList(),
                normalizedPage,
                normalizedSize,
                orders.size()
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailVO getOrderDetail(Long orderId) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        TradeOrderEntity order = tradeOrderMapper.selectOne(
                Wrappers.<TradeOrderEntity>lambdaQuery()
                        .eq(TradeOrderEntity::getId, orderId)
                        .eq(TradeOrderEntity::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_NOT_FOUND);
        }

        List<OrderItemVO> items = tradeOrderItemMapper.selectList(
                        Wrappers.<TradeOrderItemEntity>lambdaQuery()
                                .eq(TradeOrderItemEntity::getOrderId, order.getId())
                                .eq(TradeOrderItemEntity::getUserId, userId)
                                .orderByAsc(TradeOrderItemEntity::getId)
                )
                .stream()
                .map(this::toOrderItemVO)
                .toList();

        PaymentRecordEntity paymentRecord = paymentRecordMapper.selectOne(
                Wrappers.<PaymentRecordEntity>lambdaQuery()
                        .eq(PaymentRecordEntity::getOrderId, order.getId())
                        .eq(PaymentRecordEntity::getUserId, userId)
        );

        return new OrderDetailVO(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                toOrderAddressVO(order),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCanceledAt(),
                items,
                toPaymentVO(paymentRecord)
        );
    }

    @Transactional
    public OrderStatusVO cancelOrder(Long orderId) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        TradeOrderEntity order = tradeOrderMapper.selectOne(
                Wrappers.<TradeOrderEntity>lambdaQuery()
                        .eq(TradeOrderEntity::getId, orderId)
                        .eq(TradeOrderEntity::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_NOT_FOUND);
        }
        if (!Integer.valueOf(STATUS_PENDING_PAYMENT).equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED);
        }

        List<TradeOrderItemEntity> orderItems = tradeOrderItemMapper.selectList(
                Wrappers.<TradeOrderItemEntity>lambdaQuery()
                        .eq(TradeOrderItemEntity::getOrderId, order.getId())
                        .eq(TradeOrderItemEntity::getUserId, userId)
                        .orderByAsc(TradeOrderItemEntity::getId)
        );

        LocalDateTime canceledAt = LocalDateTime.now();
        int affectedRows = tradeOrderMapper.cancelPendingOrder(
                order.getId(),
                userId,
                STATUS_PENDING_PAYMENT,
                STATUS_CANCELED,
                canceledAt
        );
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED);
        }

        for (TradeOrderItemEntity orderItem : orderItems) {
            inventoryService.releaseLockedStock(orderItem.getSkuId(), orderItem.getQuantity());
        }

        behaviorEventPublishSupport.publishAfterCommit(orderCanceledCommand(userId, order, orderItems, canceledAt));
        return new OrderStatusVO(order.getId(), order.getOrderNo(), STATUS_CANCELED, canceledAt);
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

    private OrderListItemVO toOrderListItemVO(TradeOrderEntity order) {
        return new OrderListItemVO(
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getCanceledAt()
        );
    }

    private OrderAddressVO toOrderAddressVO(TradeOrderEntity order) {
        return new OrderAddressVO(
                order.getAddressId(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getProvince(),
                order.getCity(),
                order.getDistrict(),
                order.getDetailAddress(),
                order.getPostalCode()
        );
    }

    private PaymentVO toPaymentVO(PaymentRecordEntity paymentRecord) {
        if (paymentRecord == null) {
            return null;
        }
        return new PaymentVO(
                paymentRecord.getPaymentNo(),
                paymentRecord.getOrderId(),
                paymentRecord.getOrderNo(),
                paymentRecord.getAmount(),
                paymentRecord.getChannel(),
                paymentRecord.getStatus(),
                paymentRecord.getPaidAt()
        );
    }

    private int normalizePage(Integer page) {
        if (page == null || page < DEFAULT_PAGE) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }

    private BehaviorEventPublishCommand orderCreatedCommand(
            Long userId,
            TradeOrderEntity order,
            List<OrderItemVO> items
    ) {
        return new BehaviorEventPublishCommand(
                BehaviorEventType.ORDER_CREATED,
                userId,
                SOURCE_MODULE,
                "ORDER",
                order.getId(),
                null,
                firstSkuId(items),
                firstSpuId(items),
                firstCategoryId(items),
                order.getId(),
                order.getTotalAmount(),
                orderPayload(order, STATUS_PENDING_PAYMENT, order.getCreatedAt(), items)
        );
    }

    private BehaviorEventPublishCommand orderCanceledCommand(
            Long userId,
            TradeOrderEntity order,
            List<TradeOrderItemEntity> orderItems,
            LocalDateTime canceledAt
    ) {
        List<OrderItemVO> itemVOs = orderItems.stream()
                .map(this::toOrderItemVO)
                .toList();
        return new BehaviorEventPublishCommand(
                BehaviorEventType.ORDER_CANCELED,
                userId,
                SOURCE_MODULE,
                "ORDER",
                order.getId(),
                null,
                firstSkuId(itemVOs),
                firstSpuId(itemVOs),
                firstCategoryId(itemVOs),
                order.getId(),
                order.getTotalAmount(),
                orderPayload(order, STATUS_CANCELED, canceledAt, itemVOs)
        );
    }

    private Map<String, Object> orderPayload(
            TradeOrderEntity order,
            Integer status,
            LocalDateTime occurredAt,
            List<OrderItemVO> items
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "orderId", order.getId());
        putIfPresent(payload, "orderNo", order.getOrderNo());
        putIfPresent(payload, "totalAmount", order.getTotalAmount());
        putIfPresent(payload, "status", status);
        putIfPresent(payload, "occurredAt", occurredAt);
        payload.put("items", items.stream()
                .map(this::orderItemPayload)
                .toList());
        return payload;
    }

    private Map<String, Object> orderItemPayload(OrderItemVO item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "skuId", item.skuId());
        putIfPresent(payload, "spuId", item.spuId());
        putIfPresent(payload, "categoryId", item.categoryId());
        putIfPresent(payload, "categoryName", item.categoryName());
        putIfPresent(payload, "productName", item.productName());
        putIfPresent(payload, "skuName", item.skuName());
        putIfPresent(payload, "unitPrice", item.unitPrice());
        putIfPresent(payload, "quantity", item.quantity());
        putIfPresent(payload, "subtotal", item.subtotal());
        return payload;
    }

    private Long firstSkuId(List<OrderItemVO> items) {
        return items.isEmpty() ? null : items.get(0).skuId();
    }

    private Long firstSpuId(List<OrderItemVO> items) {
        return items.isEmpty() ? null : items.get(0).spuId();
    }

    private Long firstCategoryId(List<OrderItemVO> items) {
        return items.isEmpty() ? null : items.get(0).categoryId();
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private record PreparedOrderItem(
            ProductSnapshot productSnapshot,
            Integer quantity,
            BigDecimal subtotal
    ) {
    }
}
