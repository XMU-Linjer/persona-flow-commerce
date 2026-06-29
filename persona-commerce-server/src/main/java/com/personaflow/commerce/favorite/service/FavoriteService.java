package com.personaflow.commerce.favorite.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishCommand;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishSupport;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class FavoriteService {

    private static final String SOURCE_MODULE = "shopping";

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final FavoriteMapper favoriteMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProductQueryApi productQueryApi;
    private final BehaviorEventPublishSupport behaviorEventPublishSupport;

    public FavoriteService(
            FavoriteMapper favoriteMapper,
            CurrentUserProvider currentUserProvider,
            ProductQueryApi productQueryApi,
            BehaviorEventPublishSupport behaviorEventPublishSupport
    ) {
        this.favoriteMapper = favoriteMapper;
        this.currentUserProvider = currentUserProvider;
        this.productQueryApi = productQueryApi;
        this.behaviorEventPublishSupport = behaviorEventPublishSupport;
    }

    @Transactional
    public FavoriteStatusVO addFavorite(Long skuId) {
        Long userId = currentUserProvider.requireCurrentUser().userId();
        ProductSnapshot snapshot = productQueryApi.requireSellableSku(skuId);

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
            return new FavoriteStatusVO(skuId, true);
        }
        behaviorEventPublishSupport.publish(favoriteCommand(
                BehaviorEventType.FAVORITE_ADD,
                userId,
                skuId,
                snapshot
        ));
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
        behaviorEventPublishSupport.publish(favoriteCommand(
                BehaviorEventType.FAVORITE_REMOVE,
                userId,
                skuId,
                null
        ));
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

    private BehaviorEventPublishCommand favoriteCommand(
            BehaviorEventType eventType,
            Long userId,
            Long skuId,
            ProductSnapshot snapshot
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("skuId", skuId);
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
                eventType,
                userId,
                SOURCE_MODULE,
                "SKU",
                skuId,
                null,
                skuId,
                snapshot == null ? null : snapshot.spuId(),
                snapshot == null ? null : snapshot.categoryId(),
                null,
                snapshot == null ? null : snapshot.unitPrice(),
                payload
        );
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
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
