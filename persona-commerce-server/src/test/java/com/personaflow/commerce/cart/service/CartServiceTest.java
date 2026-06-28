package com.personaflow.commerce.cart.service;

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
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ProductQueryApi productQueryApi;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemMapper, currentUserProvider, productQueryApi);
    }

    @Test
    void addCartItemInsertsNewItem() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(null);
        when(cartItemMapper.insert(any(CartItemEntity.class))).thenAnswer(invocation -> {
            CartItemEntity cartItem = invocation.getArgument(0);
            cartItem.setId(20L);
            return 1;
        });

        CartItemSimpleVO result = cartService.addCartItem(new AddCartItemRequest(30001L, 2));

        assertThat(result.cartItemId()).isEqualTo(20L);
        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.quantity()).isEqualTo(2);
        verify(productQueryApi).requireSellableSku(30001L);

        ArgumentCaptor<CartItemEntity> cartItemCaptor = ArgumentCaptor.forClass(CartItemEntity.class);
        verify(cartItemMapper).insert(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getUserId()).isEqualTo(10001L);
        assertThat(cartItemCaptor.getValue().getSkuId()).isEqualTo(30001L);
        assertThat(cartItemCaptor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void addCartItemAccumulatesQuantityForExistingSku() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(10L, 30001L, 1));
        when(cartItemMapper.updateById(any(CartItemEntity.class))).thenReturn(1);

        CartItemSimpleVO result = cartService.addCartItem(new AddCartItemRequest(30001L, 2));

        assertThat(result.cartItemId()).isEqualTo(10L);
        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.quantity()).isEqualTo(3);
        verify(productQueryApi).requireSellableSku(30001L);
        verify(cartItemMapper, never()).insert(any(CartItemEntity.class));

        ArgumentCaptor<CartItemEntity> cartItemCaptor = ArgumentCaptor.forClass(CartItemEntity.class);
        verify(cartItemMapper).updateById(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void addCartItemRejectsInvalidQuantity() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());

        assertBusinessError(
                () -> cartService.addCartItem(new AddCartItemRequest(30001L, 0)),
                ErrorCode.SHOPPING_INVALID_QUANTITY
        );
        verifyNoInteractions(productQueryApi);
        verify(cartItemMapper, never()).selectOne(any());
    }

    @Test
    void updateCartItemUpdatesOwnedItem() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(10L, 30001L, 1));
        when(cartItemMapper.updateById(any(CartItemEntity.class))).thenReturn(1);

        CartItemSimpleVO result = cartService.updateCartItem(10L, new UpdateCartItemRequest(3));

        assertThat(result.cartItemId()).isEqualTo(10L);
        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.quantity()).isEqualTo(3);
        verifyNoInteractions(productQueryApi);

        ArgumentCaptor<CartItemEntity> cartItemCaptor = ArgumentCaptor.forClass(CartItemEntity.class);
        verify(cartItemMapper).updateById(cartItemCaptor.capture());
        assertThat(cartItemCaptor.getValue().getQuantity()).isEqualTo(3);
    }

    @Test
    void updateCartItemRejectsMissingOrForeignItem() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> cartService.updateCartItem(99L, new UpdateCartItemRequest(3)),
                ErrorCode.SHOPPING_CART_ITEM_NOT_FOUND
        );
        verifyNoInteractions(productQueryApi);
        verify(cartItemMapper, never()).updateById(any(CartItemEntity.class));
    }

    @Test
    void updateCartItemRejectsInvalidQuantityWithoutCallingProductQueryApi() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(10L, 30001L, 1));

        assertBusinessError(
                () -> cartService.updateCartItem(10L, new UpdateCartItemRequest(0)),
                ErrorCode.SHOPPING_INVALID_QUANTITY
        );
        verifyNoInteractions(productQueryApi);
        verify(cartItemMapper, never()).updateById(any(CartItemEntity.class));
    }

    @Test
    void deleteCartItemDeletesOwnedItem() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(cartItem(10L, 30001L, 1));
        when(cartItemMapper.delete(any())).thenReturn(1);

        cartService.deleteCartItem(10L);

        verify(cartItemMapper).delete(any());
        verifyNoInteractions(productQueryApi);
    }

    @Test
    void deleteCartItemRejectsMissingOrForeignItem() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectOne(any())).thenReturn(null);

        assertBusinessError(
                () -> cartService.deleteCartItem(99L),
                ErrorCode.SHOPPING_CART_ITEM_NOT_FOUND
        );
        verifyNoInteractions(productQueryApi);
        verify(cartItemMapper, never()).delete(any());
    }

    @Test
    void clearCartDeletesAllCurrentUserItems() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.delete(any())).thenReturn(2);

        cartService.clearCart();

        verify(cartItemMapper).delete(any());
        verifyNoInteractions(productQueryApi);
    }

    @Test
    void listCartItemsReturnsProductSnapshotsAndSubtotal() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectList(any())).thenReturn(List.of(
                cartItem(10L, 30001L, 2),
                cartItem(11L, 30002L, 3)
        ));
        when(productQueryApi.requireSellableSkus(List.of(30001L, 30002L)))
                .thenReturn(snapshotMap());

        List<CartItemVO> result = cartService.listCartItems();

        assertThat(result).hasSize(2);
        CartItemVO first = result.get(0);
        assertThat(first.cartItemId()).isEqualTo(10L);
        assertThat(first.skuId()).isEqualTo(30001L);
        assertThat(first.productName()).isEqualTo("KeyForge K3");
        assertThat(first.skuName()).isEqualTo("青轴 白色");
        assertThat(first.unitPrice()).isEqualByComparingTo("459.00");
        assertThat(first.quantity()).isEqualTo(2);
        assertThat(first.subtotal()).isEqualByComparingTo("918.00");
    }

    @Test
    void listCartItemsDoesNotCallProductQueryApiWhenCartIsEmpty() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(cartItemMapper.selectList(any())).thenReturn(List.of());

        List<CartItemVO> result = cartService.listCartItems();

        assertThat(result).isEmpty();
        verifyNoInteractions(productQueryApi);
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private CartItemEntity cartItem(Long id, Long skuId, Integer quantity) {
        CartItemEntity cartItem = new CartItemEntity();
        cartItem.setId(id);
        cartItem.setUserId(10001L);
        cartItem.setSkuId(skuId);
        cartItem.setQuantity(quantity);
        cartItem.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, id.intValue() % 60));
        cartItem.setUpdatedAt(LocalDateTime.of(2026, 6, 28, 11, id.intValue() % 60));
        return cartItem;
    }

    private Map<Long, ProductSnapshot> snapshotMap() {
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
