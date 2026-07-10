package com.personaflow.commerce.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishCommand;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishSupport;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.inventory.service.InventoryService;
import com.personaflow.commerce.inventory.service.RedisStockService;
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
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    public static final int PAYMENT_STATUS_SUCCESS = 20;
    private static final String CHANNEL_MOCK = "MOCK";
    private static final String SOURCE_MODULE = "trade";

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CurrentUserProvider currentUserProvider;
    private final InventoryService inventoryService;
    private final RedisStockService redisStockService;
    private final PaymentNoGenerator paymentNoGenerator;
    private final BehaviorEventPublishSupport behaviorEventPublishSupport;

    public PaymentService(
            TradeOrderMapper tradeOrderMapper,
            TradeOrderItemMapper tradeOrderItemMapper,
            PaymentRecordMapper paymentRecordMapper,
            CurrentUserProvider currentUserProvider,
            InventoryService inventoryService,
            RedisStockService redisStockService,
            PaymentNoGenerator paymentNoGenerator,
            BehaviorEventPublishSupport behaviorEventPublishSupport
    ) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.currentUserProvider = currentUserProvider;
        this.inventoryService = inventoryService;
        this.redisStockService = redisStockService;
        this.paymentNoGenerator = paymentNoGenerator;
        this.behaviorEventPublishSupport = behaviorEventPublishSupport;
    }

    @Transactional
    public PaymentVO payOrder(Long orderId, PayOrderRequest request) {
        String channel = normalizeChannel(request);
        Long userId = currentUserProvider.requireCurrentUser().userId();
        TradeOrderEntity order = tradeOrderMapper.selectOne(
                Wrappers.<TradeOrderEntity>lambdaQuery()
                        .eq(TradeOrderEntity::getId, orderId)
                        .eq(TradeOrderEntity::getUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_NOT_FOUND);
        }
        if (!Integer.valueOf(OrderService.STATUS_PENDING_PAYMENT).equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED);
        }

        List<TradeOrderItemEntity> orderItems = tradeOrderItemMapper.selectList(
                Wrappers.<TradeOrderItemEntity>lambdaQuery()
                        .eq(TradeOrderItemEntity::getOrderId, order.getId())
                        .eq(TradeOrderItemEntity::getUserId, userId)
                        .orderByAsc(TradeOrderItemEntity::getId)
        );

        LocalDateTime paidAt = LocalDateTime.now();
        int affectedRows = tradeOrderMapper.markOrderPaid(
                order.getId(),
                userId,
                OrderService.STATUS_PENDING_PAYMENT,
                OrderService.STATUS_PAID,
                paidAt
        );
        if (affectedRows != 1) {
            throw new BusinessException(ErrorCode.TRADE_ORDER_STATUS_NOT_ALLOWED);
        }

        PaymentRecordEntity paymentRecord = toPaymentRecord(order, userId, channel, paidAt);
        try {
            expectOneRow(paymentRecordMapper.insert(paymentRecord), "Failed to create payment record");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.TRADE_PAYMENT_RECORD_EXISTS);
        }

        for (TradeOrderItemEntity orderItem : orderItems) {
            inventoryService.confirmLockedStock(orderItem.getSkuId(), orderItem.getQuantity());
        }

        runAfterCommitBestEffort(() -> {
            for (TradeOrderItemEntity orderItem : orderItems) {
                redisStockService.confirmSoldStock(orderItem.getSkuId(), orderItem.getQuantity());
            }
        });

        behaviorEventPublishSupport.publishAfterCommit(paymentSuccessCommand(userId, order, orderItems, paymentRecord));
        return toPaymentVO(paymentRecord);
    }

    private String normalizeChannel(PayOrderRequest request) {
        if (request == null || request.channel() == null || request.channel().isBlank()) {
            return CHANNEL_MOCK;
        }
        String channel = request.channel().trim().toUpperCase(Locale.ROOT);
        if (!CHANNEL_MOCK.equals(channel)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "Unsupported payment channel");
        }
        return channel;
    }

    private PaymentRecordEntity toPaymentRecord(
            TradeOrderEntity order,
            Long userId,
            String channel,
            LocalDateTime paidAt
    ) {
        PaymentRecordEntity paymentRecord = new PaymentRecordEntity();
        paymentRecord.setPaymentNo(paymentNoGenerator.generate());
        paymentRecord.setOrderId(order.getId());
        paymentRecord.setOrderNo(order.getOrderNo());
        paymentRecord.setUserId(userId);
        paymentRecord.setAmount(order.getTotalAmount());
        paymentRecord.setChannel(channel);
        paymentRecord.setStatus(PAYMENT_STATUS_SUCCESS);
        paymentRecord.setPaidAt(paidAt);
        paymentRecord.setCreatedAt(paidAt);
        paymentRecord.setUpdatedAt(paidAt);
        return paymentRecord;
    }

    private PaymentVO toPaymentVO(PaymentRecordEntity paymentRecord) {
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

    private void expectOneRow(int affectedRows, String message) {
        if (affectedRows != 1) {
            throw new IllegalStateException(message);
        }
    }

    private void runAfterCommitBestEffort(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runBestEffort(action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runBestEffort(action);
            }
        });
    }

    private void runBestEffort(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to synchronize paid stock to Redis; MySQL remains the stock source of truth");
        }
    }

    private BehaviorEventPublishCommand paymentSuccessCommand(
            Long userId,
            TradeOrderEntity order,
            List<TradeOrderItemEntity> orderItems,
            PaymentRecordEntity paymentRecord
    ) {
        List<Map<String, Object>> itemPayloads = orderItems.stream()
                .map(this::orderItemPayload)
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "orderId", order.getId());
        putIfPresent(payload, "orderNo", order.getOrderNo());
        putIfPresent(payload, "totalAmount", order.getTotalAmount());
        putIfPresent(payload, "paymentNo", paymentRecord.getPaymentNo());
        putIfPresent(payload, "channel", paymentRecord.getChannel());
        putIfPresent(payload, "status", PAYMENT_STATUS_SUCCESS);
        putIfPresent(payload, "paidAt", paymentRecord.getPaidAt());
        payload.put("preferenceConfirmed", true);
        payload.put("needFulfilled", true);
        payload.put("complementTriggered", true);
        payload.put("items", itemPayloads);

        return new BehaviorEventPublishCommand(
                BehaviorEventType.PAYMENT_SUCCESS,
                userId,
                SOURCE_MODULE,
                "ORDER",
                order.getId(),
                null,
                firstSkuId(orderItems),
                firstSpuId(orderItems),
                firstCategoryId(orderItems),
                order.getId(),
                paymentRecord.getAmount(),
                payload
        );
    }

    private Map<String, Object> orderItemPayload(TradeOrderItemEntity item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "skuId", item.getSkuId());
        putIfPresent(payload, "spuId", item.getSpuId());
        putIfPresent(payload, "categoryId", item.getCategoryId());
        putIfPresent(payload, "categoryName", item.getCategoryName());
        putIfPresent(payload, "productName", item.getProductName());
        putIfPresent(payload, "skuName", item.getSkuName());
        putIfPresent(payload, "unitPrice", item.getUnitPrice());
        putIfPresent(payload, "quantity", item.getQuantity());
        putIfPresent(payload, "subtotal", item.getSubtotal());
        return payload;
    }

    private Long firstSkuId(List<TradeOrderItemEntity> items) {
        return items.isEmpty() ? null : items.get(0).getSkuId();
    }

    private Long firstSpuId(List<TradeOrderItemEntity> items) {
        return items.isEmpty() ? null : items.get(0).getSpuId();
    }

    private Long firstCategoryId(List<TradeOrderItemEntity> items) {
        return items.isEmpty() ? null : items.get(0).getCategoryId();
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }
}
