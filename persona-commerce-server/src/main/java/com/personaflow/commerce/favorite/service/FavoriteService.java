package com.personaflow.commerce.favorite.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.favorite.entity.FavoriteEntity;
import com.personaflow.commerce.favorite.mapper.FavoriteMapper;
import com.personaflow.commerce.favorite.vo.FavoriteItemVO;
import com.personaflow.commerce.favorite.vo.FavoriteStatusVO;
import com.personaflow.commerce.product.api.ProductQueryApi;
import com.personaflow.commerce.product.api.model.ProductSnapshot;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class FavoriteService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final FavoriteMapper favoriteMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProductQueryApi productQueryApi;

    public FavoriteService(
            FavoriteMapper favoriteMapper,
            CurrentUserProvider currentUserProvider,
            ProductQueryApi productQueryApi
    ) {
        this.favoriteMapper = favoriteMapper;
        this.currentUserProvider = currentUserProvider;
        this.productQueryApi = productQueryApi;
    }

    @Transactional
    public FavoriteStatusVO addFavorite(Long skuId) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        productQueryApi.requireSellableSku(skuId);

        FavoriteEntity existingFavorite = favoriteMapper.selectOne(
                Wrappers.<FavoriteEntity>lambdaQuery()
                        .eq(FavoriteEntity::getUserId, userId)
                        .eq(FavoriteEntity::getSkuId, skuId)
        );
        if (existingFavorite != null) {
            return new FavoriteStatusVO(skuId, true);
        }

        FavoriteEntity favorite = new FavoriteEntity();
        favorite.setUserId(userId);
        favorite.setSkuId(skuId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException exception) {
            // Concurrent duplicate favorite creation is treated as idempotent success.
        }
        return new FavoriteStatusVO(skuId, true);
    }

    @Transactional
    public FavoriteStatusVO removeFavorite(Long skuId) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        favoriteMapper.delete(
                Wrappers.<FavoriteEntity>lambdaQuery()
                        .eq(FavoriteEntity::getUserId, userId)
                        .eq(FavoriteEntity::getSkuId, skuId)
        );
        return new FavoriteStatusVO(skuId, false);
    }

    @Transactional(readOnly = true)
    public PageResult<FavoriteItemVO> listFavorites(Integer page, Integer size) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        List<FavoriteEntity> favorites = favoriteMapper.selectList(
                Wrappers.<FavoriteEntity>lambdaQuery()
                        .eq(FavoriteEntity::getUserId, userId)
                        .orderByDesc(FavoriteEntity::getCreatedAt)
                        .orderByDesc(FavoriteEntity::getId)
        );
        if (favorites.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0);
        }

        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, favorites.size());
        int toIndex = Math.min(fromIndex + normalizedSize, favorites.size());
        List<FavoriteEntity> pageFavorites = favorites.subList(fromIndex, toIndex);
        if (pageFavorites.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, favorites.size());
        }

        Map<Long, ProductSnapshot> snapshots = productQueryApi.requireSellableSkus(
                pageFavorites.stream()
                        .map(FavoriteEntity::getSkuId)
                        .toList()
        );

        return new PageResult<>(
                pageFavorites.stream()
                        .map(favorite -> toFavoriteItemVO(favorite, snapshots))
                        .toList(),
                normalizedPage,
                normalizedSize,
                favorites.size()
        );
    }

    private FavoriteItemVO toFavoriteItemVO(
            FavoriteEntity favorite,
            Map<Long, ProductSnapshot> snapshots
    ) {
        ProductSnapshot snapshot = snapshots.get(favorite.getSkuId());
        return new FavoriteItemVO(
                favorite.getId(),
                favorite.getSkuId(),
                snapshot.spuId(),
                snapshot.categoryId(),
                snapshot.categoryName(),
                snapshot.productName(),
                snapshot.skuName(),
                snapshot.unitPrice(),
                snapshot.imageUrl(),
                favorite.getCreatedAt()
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
}
