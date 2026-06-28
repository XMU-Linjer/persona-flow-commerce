package com.personaflow.commerce.cart.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.cart.dto.AddCartItemRequest;
import com.personaflow.commerce.cart.dto.UpdateCartItemRequest;
import com.personaflow.commerce.cart.entity.CartItemEntity;
import com.personaflow.commerce.cart.mapper.CartItemMapper;
import com.personaflow.commerce.cart.vo.CartItemSimpleVO;
import com.personaflow.commerce.cart.vo.CartItemVO;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.product.api.ProductQueryApi;
import com.personaflow.commerce.product.api.model.ProductSnapshot;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProductQueryApi productQueryApi;

    public CartService(
            CartItemMapper cartItemMapper,
            CurrentUserProvider currentUserProvider,
            ProductQueryApi productQueryApi
    ) {
        this.cartItemMapper = cartItemMapper;
        this.currentUserProvider = currentUserProvider;
        this.productQueryApi = productQueryApi;
    }

    @Transactional
    public CartItemSimpleVO addCartItem(AddCartItemRequest request) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        validateQuantity(request.quantity());
        productQueryApi.requireSellableSku(request.skuId());

        CartItemEntity existingItem = cartItemMapper.selectOne(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getUserId, userId)
                        .eq(CartItemEntity::getSkuId, request.skuId())
        );
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            cartItemMapper.updateById(existingItem);
            return toSimpleVO(existingItem);
        }

        CartItemEntity cartItem = new CartItemEntity();
        cartItem.setUserId(userId);
        cartItem.setSkuId(request.skuId());
        cartItem.setQuantity(request.quantity());
        cartItemMapper.insert(cartItem);
        return toSimpleVO(cartItem);
    }

    @Transactional
    public CartItemSimpleVO updateCartItem(Long cartItemId, UpdateCartItemRequest request) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        CartItemEntity cartItem = requireOwnedCartItem(userId, cartItemId);
        validateQuantity(request.quantity());
        cartItem.setQuantity(request.quantity());
        cartItemMapper.updateById(cartItem);
        return toSimpleVO(cartItem);
    }

    @Transactional
    public void deleteCartItem(Long cartItemId) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        requireOwnedCartItem(userId, cartItemId);
        int deleted = cartItemMapper.delete(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getId, cartItemId)
                        .eq(CartItemEntity::getUserId, userId)
        );
        if (deleted <= 0) {
            throw new BusinessException(ErrorCode.SHOPPING_CART_ITEM_NOT_FOUND);
        }
    }

    @Transactional
    public void clearCart() {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        cartItemMapper.delete(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getUserId, userId)
        );
    }

    @Transactional(readOnly = true)
    public List<CartItemVO> listCartItems() {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        List<CartItemEntity> cartItems = cartItemMapper.selectList(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getUserId, userId)
                        .orderByDesc(CartItemEntity::getCreatedAt)
                        .orderByDesc(CartItemEntity::getId)
        );
        if (cartItems.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductSnapshot> snapshots = productQueryApi.requireSellableSkus(
                cartItems.stream()
                        .map(CartItemEntity::getSkuId)
                        .toList()
        );
        return cartItems.stream()
                .map(cartItem -> toCartItemVO(cartItem, snapshots))
                .toList();
    }

    private CartItemEntity requireOwnedCartItem(Long userId, Long cartItemId) {
        CartItemEntity cartItem = cartItemMapper.selectOne(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getId, cartItemId)
                        .eq(CartItemEntity::getUserId, userId)
        );
        if (cartItem == null) {
            throw new BusinessException(ErrorCode.SHOPPING_CART_ITEM_NOT_FOUND);
        }
        return cartItem;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.SHOPPING_INVALID_QUANTITY);
        }
    }

    private CartItemSimpleVO toSimpleVO(CartItemEntity cartItem) {
        return new CartItemSimpleVO(
                cartItem.getId(),
                cartItem.getSkuId(),
                cartItem.getQuantity()
        );
    }

    private CartItemVO toCartItemVO(
            CartItemEntity cartItem,
            Map<Long, ProductSnapshot> snapshots
    ) {
        ProductSnapshot snapshot = snapshots.get(cartItem.getSkuId());
        BigDecimal subtotal = snapshot.unitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        return new CartItemVO(
                cartItem.getId(),
                cartItem.getSkuId(),
                snapshot.spuId(),
                snapshot.categoryId(),
                snapshot.categoryName(),
                snapshot.productName(),
                snapshot.skuName(),
                snapshot.unitPrice(),
                snapshot.imageUrl(),
                cartItem.getQuantity(),
                subtotal,
                cartItem.getCreatedAt(),
                cartItem.getUpdatedAt()
        );
    }
}
