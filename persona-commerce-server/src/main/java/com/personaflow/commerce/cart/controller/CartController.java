package com.personaflow.commerce.cart.controller;

import com.personaflow.commerce.cart.dto.AddCartItemRequest;
import com.personaflow.commerce.cart.dto.UpdateCartItemRequest;
import com.personaflow.commerce.cart.service.CartService;
import com.personaflow.commerce.cart.vo.CartItemSimpleVO;
import com.personaflow.commerce.cart.vo.CartItemVO;
import com.personaflow.commerce.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/shopping/cart/items")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ApiResponse<CartItemSimpleVO> addCartItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success(cartService.addCartItem(request));
    }

    @PatchMapping("/{cartItemId}")
    public ApiResponse<CartItemSimpleVO> updateCartItem(
            @Positive @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success(cartService.updateCartItem(cartItemId, request));
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<Void> deleteCartItem(@Positive @PathVariable Long cartItemId) {
        cartService.deleteCartItem(cartItemId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        cartService.clearCart();
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<CartItemVO>> listCartItems() {
        return ApiResponse.success(cartService.listCartItems());
    }
}
