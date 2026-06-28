package com.personaflow.commerce.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.product.api.ProductQueryApi;
import com.personaflow.commerce.product.api.model.ProductSnapshot;
import com.personaflow.commerce.product.entity.ProductCategoryEntity;
import com.personaflow.commerce.product.entity.ProductSkuEntity;
import com.personaflow.commerce.product.entity.ProductSpuEntity;
import com.personaflow.commerce.product.mapper.ProductCategoryMapper;
import com.personaflow.commerce.product.mapper.ProductSkuMapper;
import com.personaflow.commerce.product.mapper.ProductSpuMapper;
import com.personaflow.commerce.product.vo.CategoryVO;
import com.personaflow.commerce.product.vo.ProductDetailVO;
import com.personaflow.commerce.product.vo.ProductListItemVO;
import com.personaflow.commerce.product.vo.SkuDetailVO;
import com.personaflow.commerce.product.vo.SkuVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService implements ProductQueryApi {

    private static final int ENABLED_STATUS = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> STRING_OBJECT_MAP_TYPE = new TypeReference<>() {
    };

    private final ProductCategoryMapper categoryMapper;
    private final ProductSpuMapper spuMapper;
    private final ProductSkuMapper skuMapper;
    private final ObjectMapper objectMapper;

    public ProductService(
            ProductCategoryMapper categoryMapper,
            ProductSpuMapper spuMapper,
            ProductSkuMapper skuMapper,
            ObjectMapper objectMapper
    ) {
        this.categoryMapper = categoryMapper;
        this.spuMapper = spuMapper;
        this.skuMapper = skuMapper;
        this.objectMapper = objectMapper;
    }

    public List<CategoryVO> listCategoryTree() {
        List<ProductCategoryEntity> categories = categoryMapper.selectList(
                Wrappers.<ProductCategoryEntity>lambdaQuery()
                        .eq(ProductCategoryEntity::getStatus, ENABLED_STATUS)
                        .orderByAsc(ProductCategoryEntity::getSortOrder)
                        .orderByAsc(ProductCategoryEntity::getId)
        );

        Map<Long, CategoryVO> categoryMap = new LinkedHashMap<>();
        for (ProductCategoryEntity category : categories) {
            categoryMap.put(category.getId(), toCategoryVO(category));
        }

        List<CategoryVO> roots = new ArrayList<>();
        for (CategoryVO category : categoryMap.values()) {
            if (category.parentId() == null) {
                roots.add(category);
                continue;
            }

            CategoryVO parent = categoryMap.get(category.parentId());
            if (parent != null) {
                parent.children().add(category);
            }
        }
        return roots;
    }

    public PageResult<ProductListItemVO> listProducts(
            Long categoryId,
            String keyword,
            Integer page,
            Integer size
    ) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        String normalizedKeyword = normalizeOptionalText(keyword);

        List<ProductSpuEntity> spus = spuMapper.selectList(
                Wrappers.<ProductSpuEntity>lambdaQuery()
                        .eq(ProductSpuEntity::getStatus, ENABLED_STATUS)
                        .eq(categoryId != null, ProductSpuEntity::getCategoryId, categoryId)
                        .and(normalizedKeyword != null, wrapper -> wrapper
                                .like(ProductSpuEntity::getName, normalizedKeyword)
                                .or()
                                .like(ProductSpuEntity::getSubtitle, normalizedKeyword)
                                .or()
                                .like(ProductSpuEntity::getBrand, normalizedKeyword)
                        )
                        .orderByAsc(ProductSpuEntity::getSortOrder)
                        .orderByAsc(ProductSpuEntity::getId)
        );

        if (spus.isEmpty()) {
            return new PageResult<>(List.of(), normalizedPage, normalizedSize, 0);
        }

        Map<Long, List<ProductSkuEntity>> skusBySpuId = loadSellableSkusBySpuId(
                spus.stream().map(ProductSpuEntity::getId).toList()
        );
        Map<Long, ProductCategoryEntity> categoryMap = loadEnabledCategories(
                spus.stream().map(ProductSpuEntity::getCategoryId).collect(Collectors.toSet())
        );

        List<ProductListItemVO> allRecords = spus.stream()
                .map(spu -> toProductListItemVO(
                        spu,
                        categoryMap.get(spu.getCategoryId()),
                        skusBySpuId.getOrDefault(spu.getId(), List.of())
                ))
                .filter(Objects::nonNull)
                .toList();

        int fromIndex = Math.min((normalizedPage - 1) * normalizedSize, allRecords.size());
        int toIndex = Math.min(fromIndex + normalizedSize, allRecords.size());
        return new PageResult<>(
                allRecords.subList(fromIndex, toIndex),
                normalizedPage,
                normalizedSize,
                allRecords.size()
        );
    }

    public ProductDetailVO getProductDetail(Long spuId) {
        ProductSpuEntity spu = requireSellableSpu(spuId);
        ProductCategoryEntity category = requireEnabledCategory(spu.getCategoryId());
        List<SkuVO> skus = skuMapper.selectList(
                        Wrappers.<ProductSkuEntity>lambdaQuery()
                                .eq(ProductSkuEntity::getSpuId, spu.getId())
                                .eq(ProductSkuEntity::getStatus, ENABLED_STATUS)
                                .orderByAsc(ProductSkuEntity::getId)
                )
                .stream()
                .map(this::toSkuVO)
                .toList();

        return new ProductDetailVO(
                spu.getId(),
                spu.getCategoryId(),
                category.getName(),
                spu.getName(),
                spu.getSubtitle(),
                spu.getBrand(),
                spu.getDescription(),
                spu.getMainImageUrl(),
                parseStringList(spu.getDetailImagesJson()),
                parseObjectMap(spu.getAttributesJson()),
                spu.getTags(),
                skus
        );
    }

    public SkuDetailVO getSkuDetail(Long skuId) {
        ProductSkuEntity sku = skuMapper.selectById(skuId);
        if (sku == null || !isEnabled(sku.getStatus())) {
            throw new BusinessException(ErrorCode.CATALOG_SKU_NOT_FOUND);
        }

        ProductSpuEntity spu = requireSellableSpu(sku.getSpuId());
        ProductCategoryEntity category = requireEnabledCategory(spu.getCategoryId());
        return new SkuDetailVO(
                sku.getId(),
                spu.getId(),
                spu.getName(),
                sku.getSkuName(),
                category.getId(),
                category.getName(),
                sku.getPrice(),
                sku.getOriginalPrice(),
                sku.getImageUrl(),
                parseObjectMap(sku.getSpecsJson()),
                sku.getStatus()
        );
    }

    @Override
    public ProductSnapshot requireSellableSku(Long skuId) {
        if (skuId == null) {
            throw new BusinessException(ErrorCode.CATALOG_SKU_NOT_FOUND);
        }
        return requireSellableSkus(List.of(skuId)).get(skuId);
    }

    @Override
    public Map<Long, ProductSnapshot> requireSellableSkus(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> requestedSkuIds = skuIds.stream()
                .peek(skuId -> {
                    if (skuId == null) {
                        throw new BusinessException(ErrorCode.CATALOG_SKU_NOT_FOUND);
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedSkuIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ProductSkuEntity> skuMap = skuMapper.selectByIds(requestedSkuIds)
                .stream()
                .collect(Collectors.toMap(
                        ProductSkuEntity::getId,
                        Function.identity(),
                        (left, right) -> left
                ));
        for (Long skuId : requestedSkuIds) {
            ProductSkuEntity sku = skuMap.get(skuId);
            if (sku == null) {
                throw new BusinessException(ErrorCode.CATALOG_SKU_NOT_FOUND);
            }
            if (!isEnabled(sku.getStatus())) {
                throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE);
            }
        }

        Map<Long, ProductSpuEntity> spuMap = spuMapper.selectByIds(
                        skuMap.values()
                                .stream()
                                .map(ProductSkuEntity::getSpuId)
                                .collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(
                        ProductSpuEntity::getId,
                        Function.identity(),
                        (left, right) -> left
                ));

        for (ProductSkuEntity sku : skuMap.values()) {
            ProductSpuEntity spu = spuMap.get(sku.getSpuId());
            if (spu == null) {
                throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_FOUND);
            }
            if (!isEnabled(spu.getStatus())) {
                throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE);
            }
        }

        Map<Long, ProductCategoryEntity> categoryMap = categoryMapper.selectByIds(
                        spuMap.values()
                                .stream()
                                .map(ProductSpuEntity::getCategoryId)
                                .collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(
                        ProductCategoryEntity::getId,
                        Function.identity(),
                        (left, right) -> left
                ));

        Map<Long, ProductSnapshot> snapshots = new LinkedHashMap<>();
        for (Long skuId : requestedSkuIds) {
            ProductSkuEntity sku = skuMap.get(skuId);
            ProductSpuEntity spu = spuMap.get(sku.getSpuId());
            ProductCategoryEntity category = categoryMap.get(spu.getCategoryId());
            if (category == null || !isEnabled(category.getStatus())) {
                throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE);
            }
            snapshots.put(skuId, toProductSnapshot(sku, spu, category));
        }
        return snapshots;
    }

    private ProductSpuEntity requireSellableSpu(Long spuId) {
        ProductSpuEntity spu = spuMapper.selectById(spuId);
        if (spu == null) {
            throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_FOUND);
        }
        if (!isEnabled(spu.getStatus())) {
            throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE);
        }
        return spu;
    }

    private ProductCategoryEntity requireEnabledCategory(Long categoryId) {
        ProductCategoryEntity category = categoryMapper.selectById(categoryId);
        if (category == null || !isEnabled(category.getStatus())) {
            throw new BusinessException(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE);
        }
        return category;
    }

    private Map<Long, List<ProductSkuEntity>> loadSellableSkusBySpuId(Collection<Long> spuIds) {
        if (spuIds.isEmpty()) {
            return Map.of();
        }
        return skuMapper.selectList(
                        Wrappers.<ProductSkuEntity>lambdaQuery()
                                .in(ProductSkuEntity::getSpuId, spuIds)
                                .eq(ProductSkuEntity::getStatus, ENABLED_STATUS)
                )
                .stream()
                .collect(Collectors.groupingBy(ProductSkuEntity::getSpuId));
    }

    private Map<Long, ProductCategoryEntity> loadEnabledCategories(Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectByIds(categoryIds)
                .stream()
                .filter(category -> isEnabled(category.getStatus()))
                .collect(Collectors.toMap(
                        ProductCategoryEntity::getId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private ProductListItemVO toProductListItemVO(
            ProductSpuEntity spu,
            ProductCategoryEntity category,
            List<ProductSkuEntity> sellableSkus
    ) {
        if (category == null || sellableSkus.isEmpty()) {
            return null;
        }

        BigDecimal minPrice = sellableSkus.stream()
                .map(ProductSkuEntity::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = sellableSkus.stream()
                .map(ProductSkuEntity::getPrice)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
        int salesCount = sellableSkus.stream()
                .map(ProductSkuEntity::getSalesCount)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return new ProductListItemVO(
                spu.getId(),
                spu.getCategoryId(),
                category.getName(),
                spu.getName(),
                spu.getSubtitle(),
                spu.getBrand(),
                spu.getMainImageUrl(),
                minPrice,
                maxPrice,
                salesCount,
                spu.getTags(),
                spu.getStatus()
        );
    }

    private ProductSnapshot toProductSnapshot(
            ProductSkuEntity sku,
            ProductSpuEntity spu,
            ProductCategoryEntity category
    ) {
        return new ProductSnapshot(
                sku.getId(),
                spu.getId(),
                category.getId(),
                category.getName(),
                spu.getName(),
                sku.getSkuName(),
                sku.getPrice(),
                firstNonBlank(sku.getImageUrl(), spu.getMainImageUrl())
        );
    }

    private CategoryVO toCategoryVO(ProductCategoryEntity category) {
        return new CategoryVO(
                category.getId(),
                category.getName(),
                category.getParentId(),
                category.getLevel(),
                category.getSortOrder(),
                category.getIconUrl(),
                new ArrayList<>()
        );
    }

    private SkuVO toSkuVO(ProductSkuEntity sku) {
        return new SkuVO(
                sku.getId(),
                sku.getSkuName(),
                parseObjectMap(sku.getSpecsJson()),
                sku.getPrice(),
                sku.getOriginalPrice(),
                sku.getImageUrl(),
                sku.getStatus(),
                sku.getSalesCount()
        );
    }

    private List<String> parseStringList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid product image JSON", exception);
        }
    }

    private Map<String, Object> parseObjectMap(String json) {
        if (isBlank(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, STRING_OBJECT_MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid product attribute JSON", exception);
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isEnabled(Integer status) {
        return Integer.valueOf(ENABLED_STATUS).equals(status);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String first, String fallback) {
        return isBlank(first) ? fallback : first;
    }
}
