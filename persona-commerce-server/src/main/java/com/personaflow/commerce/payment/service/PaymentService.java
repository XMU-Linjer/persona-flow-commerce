package com.personaflow.commerce.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PaymentService {

    public static final int PAYMENT_STATUS_SUCCESS = 20;
    private static final String CHANNEL_MOCK = "MOCK";

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final CurrentUserProvider currentUserProvider;
    private final InventoryService inventoryService;
    private final PaymentNoGenerator paymentNoGenerator;

    public PaymentService(
            TradeOrderMapper tradeOrderMapper,
            TradeOrderItemMapper tradeOrderItemMapper,
            PaymentRecordMapper paymentRecordMapper,
            CurrentUserProvider currentUserProvider,
            InventoryService inventoryService,
            PaymentNoGenerator paymentNoGenerator
    ) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.tradeOrderItemMapper = tradeOrderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.currentUserProvider = currentUserProvider;
        this.inventoryService = inventoryService;
        this.paymentNoGenerator = paymentNoGenerator;
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
}
