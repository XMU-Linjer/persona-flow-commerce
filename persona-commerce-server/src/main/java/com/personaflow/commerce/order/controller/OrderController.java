package com.personaflow.commerce.order.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.order.dto.CreateOrderRequest;
import com.personaflow.commerce.order.service.OrderService;
import com.personaflow.commerce.order.vo.OrderCreateVO;
import com.personaflow.commerce.order.vo.OrderDetailVO;
import com.personaflow.commerce.order.vo.OrderListItemVO;
import com.personaflow.commerce.order.vo.OrderStatusVO;
import com.personaflow.commerce.payment.dto.PayOrderRequest;
import com.personaflow.commerce.payment.service.PaymentService;
import com.personaflow.commerce.payment.vo.PaymentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/trade/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    public OrderController(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<OrderCreateVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(request));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderListItemVO>> listOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return ApiResponse.success(orderService.listOrders(status, page, size));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDetailVO> getOrderDetail(@Positive @PathVariable Long orderId) {
        return ApiResponse.success(orderService.getOrderDetail(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderStatusVO> cancelOrder(@Positive @PathVariable Long orderId) {
        return ApiResponse.success(orderService.cancelOrder(orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<PaymentVO> payOrder(
            @Positive @PathVariable Long orderId,
            @Valid @RequestBody(required = false) PayOrderRequest request
    ) {
        return ApiResponse.success(paymentService.payOrder(orderId, request));
    }
}
