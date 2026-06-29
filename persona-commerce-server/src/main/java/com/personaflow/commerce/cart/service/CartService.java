package com.personaflow.commerce.cart.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishCommand;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishSupport;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final String SOURCE_MODULE = "shopping";

    private final CartItemMapper cartItemMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProductQueryApi productQueryApi;
    private final BehaviorEventPublishSupport behaviorEventPublishSupport;

    public CartService(
            CartItemMapper cartItemMapper,
            CurrentUserProvider currentUserProvider,
            ProductQueryApi productQueryApi,
            BehaviorEventPublishSupport behaviorEventPublishSupport
    ) {
        this.cartItemMapper = cartItemMapper;
        this.currentUserProvider = currentUserProvider;
        this.productQueryApi = productQueryApi;
        this.behaviorEventPublishSupport = behaviorEventPublishSupport;
    }

    @Transactional
    public CartItemSimpleVO addCartItem(AddCartItemRequest request) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        validateQuantity(request.quantity());
        ProductSnapshot snapshot = productQueryApi.requireSellableSku(request.skuId());

        CartItemEntity existingItem = cartItemMapper.selectOne(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getUserId, userId)
                        .eq(CartItemEntity::getSkuId, request.skuId())
        );
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            cartItemMapper.updateById(existingItem);
            behaviorEventPublishSupport.publish(cartAddCommand(userId, existingItem, request.quantity(), snapshot));
            return toSimpleVO(existingItem);
        }

        CartItemEntity cartItem = new CartItemEntity();
        cartItem.setUserId(userId);
        cartItem.setSkuId(request.skuId());
        cartItem.setQuantity(request.quantity());
        cartItemMapper.insert(cartItem);
        behaviorEventPublishSupport.publish(cartAddCommand(userId, cartItem, request.quantity(), snapshot));
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
        CartItemEntity cartItem = requireOwnedCartItem(userId, cartItemId);
        int deleted = cartItemMapper.delete(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getId, cartItemId)
                        .eq(CartItemEntity::getUserId, userId)
        );
        if (deleted <= 0) {
            throw new BusinessException(ErrorCode.SHOPPING_CART_ITEM_NOT_FOUND);
        }
        behaviorEventPublishSupport.publish(cartRemoveCommand(userId, cartItem));
    }

    @Transactional
    public void clearCart() {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        cartItemMapper.delete(
                Wrappers.<CartItemEntity>lambdaQuery()
                        .eq(CartItemEntity::getUserId, userId)
        );
        behaviorEventPublishSupport.publish(cartClearCommand(userId));
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

    private BehaviorEventPublishCommand cartAddCommand(
            Long userId,
            CartItemEntity cartItem,
            Integer addedQuantity,
            ProductSnapshot snapshot
    ) {
        Map<String, Object> payload = cartItemPayload(cartItem);
        putIfPresent(payload, "addedQuantity", addedQuantity);
        if (snapshot != null) {
            putIfPresent(payload, "spuId", snapshot.spuId());
            putIfPresent(payload, "categoryId", snapshot.categoryId());
            putIfPresent(payload, "categoryName", snapshot.categoryName());
            putIfPresent(payload, "productName", snapshot.productName());
            putIfPresent(payload, "skuName", snapshot.skuName());
            putIfPresent(payload, "price", snapshot.unitPrice());
            putIfPresent(payload, "imageUrl", snapshot.imageUrl());
        }
        return new BehaviorEventPublishCommand(
                BehaviorEventType.CART_ADD,
                userId,
                SOURCE_MODULE,
                "SKU",
                cartItem.getSkuId(),
                null,
                cartItem.getSkuId(),
                snapshot == null ? null : snapshot.spuId(),
                snapshot == null ? null : snapshot.categoryId(),
                null,
                snapshot == null ? null : snapshot.unitPrice(),
                payload
        );
    }

    private BehaviorEventPublishCommand cartRemoveCommand(Long userId, CartItemEntity cartItem) {
        return new BehaviorEventPublishCommand(
                BehaviorEventType.CART_REMOVE,
                userId,
                SOURCE_MODULE,
                "CART_ITEM",
                cartItem.getId(),
                null,
                cartItem.getSkuId(),
                null,
                null,
                null,
                null,
                cartItemPayload(cartItem)
        );
    }

    private BehaviorEventPublishCommand cartClearCommand(Long userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "clearCart");
        return new BehaviorEventPublishCommand(
                BehaviorEventType.CART_CLEAR,
                userId,
                SOURCE_MODULE,
                "CART",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                payload
        );
    }

    private Map<String, Object> cartItemPayload(CartItemEntity cartItem) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "cartItemId", cartItem.getId());
        putIfPresent(payload, "skuId", cartItem.getSkuId());
        putIfPresent(payload, "quantity", cartItem.getQuantity());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }
}
