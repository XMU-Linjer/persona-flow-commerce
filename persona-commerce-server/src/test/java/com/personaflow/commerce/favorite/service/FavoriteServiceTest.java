package com.personaflow.commerce.favorite.service;

import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.favorite.entity.FavoriteEntity;
import com.personaflow.commerce.favorite.mapper.FavoriteMapper;
import com.personaflow.commerce.favorite.vo.FavoriteItemVO;
import com.personaflow.commerce.favorite.vo.FavoriteStatusVO;
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
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteMapper favoriteMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private ProductQueryApi productQueryApi;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteMapper, currentUserProvider, productQueryApi);
    }

    @Test
    void addFavoriteInsertsFavoriteForCurrentUser() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(favoriteMapper.insert(any(FavoriteEntity.class))).thenReturn(1);

        FavoriteStatusVO result = favoriteService.addFavorite(30001L);

        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.favorited()).isTrue();
        verify(productQueryApi).requireSellableSku(30001L);

        ArgumentCaptor<FavoriteEntity> favoriteCaptor = ArgumentCaptor.forClass(FavoriteEntity.class);
        verify(favoriteMapper).insert(favoriteCaptor.capture());
        assertThat(favoriteCaptor.getValue().getUserId()).isEqualTo(10001L);
        assertThat(favoriteCaptor.getValue().getSkuId()).isEqualTo(30001L);
    }

    @Test
    void addFavoriteReturnsSuccessWhenAlreadyFavorited() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.selectOne(any())).thenReturn(favorite(10L, 30001L));

        FavoriteStatusVO result = favoriteService.addFavorite(30001L);

        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.favorited()).isTrue();
        verify(productQueryApi).requireSellableSku(30001L);
        verify(favoriteMapper, never()).insert(any(FavoriteEntity.class));
    }

    @Test
    void addFavoriteTreatsDuplicateKeyAsIdempotentSuccess() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.selectOne(any())).thenReturn(null);
        when(favoriteMapper.insert(any(FavoriteEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate favorite"));

        FavoriteStatusVO result = favoriteService.addFavorite(30001L);

        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.favorited()).isTrue();
        verify(productQueryApi).requireSellableSku(30001L);
    }

    @Test
    void removeFavoriteDeletesFavoriteForCurrentUser() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.delete(any())).thenReturn(1);

        FavoriteStatusVO result = favoriteService.removeFavorite(30001L);

        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.favorited()).isFalse();
        verify(favoriteMapper).delete(any());
        verifyNoInteractions(productQueryApi);
    }

    @Test
    void removeFavoriteReturnsSuccessWhenFavoriteDoesNotExist() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.delete(any())).thenReturn(0);

        FavoriteStatusVO result = favoriteService.removeFavorite(30001L);

        assertThat(result.skuId()).isEqualTo(30001L);
        assertThat(result.favorited()).isFalse();
        verifyNoInteractions(productQueryApi);
    }

    @Test
    void listFavoritesReturnsProductSnapshots() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.selectList(any())).thenReturn(List.of(
                favorite(10L, 30001L),
                favorite(11L, 30002L)
        ));
        when(productQueryApi.requireSellableSkus(List.of(30001L, 30002L)))
                .thenReturn(snapshotMap());

        PageResult<FavoriteItemVO> result = favoriteService.listFavorites(1, 10);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.records()).hasSize(2);
        FavoriteItemVO first = result.records().get(0);
        assertThat(first.favoriteId()).isEqualTo(10L);
        assertThat(first.skuId()).isEqualTo(30001L);
        assertThat(first.spuId()).isEqualTo(20001L);
        assertThat(first.categoryName()).isEqualTo("键盘鼠标");
        assertThat(first.productName()).isEqualTo("KeyForge K3");
        assertThat(first.skuName()).isEqualTo("青轴 白色");
        assertThat(first.unitPrice()).isEqualByComparingTo("459.00");
        assertThat(first.imageUrl()).isEqualTo("sku-30001.jpg");
    }

    @Test
    void listFavoritesDoesNotCallProductQueryApiWhenEmpty() {
        when(currentUserProvider.requireCurrentUser()).thenReturn(currentUser());
        when(favoriteMapper.selectList(any())).thenReturn(List.of());

        PageResult<FavoriteItemVO> result = favoriteService.listFavorites(1, 10);

        assertThat(result.total()).isZero();
        assertThat(result.records()).isEmpty();
        verifyNoInteractions(productQueryApi);
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }

    private FavoriteEntity favorite(Long id, Long skuId) {
        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setId(id);
        favorite.setUserId(10001L);
        favorite.setSkuId(skuId);
        favorite.setCreatedAt(LocalDateTime.of(2026, 6, 28, 10, id.intValue() % 60));
        return favorite;
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
}
