package com.personaflow.commerce.order.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.order.dto.CreateOrderRequest;
import com.personaflow.commerce.order.service.OrderService;
import com.personaflow.commerce.order.vo.OrderCreateVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/trade/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderCreateVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(request));
    }
}
