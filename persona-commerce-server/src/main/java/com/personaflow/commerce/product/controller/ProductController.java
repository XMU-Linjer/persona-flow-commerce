package com.personaflow.commerce.product.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishCommand;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishSupport;
import com.personaflow.commerce.product.service.ProductService;
import com.personaflow.commerce.product.vo.CategoryVO;
import com.personaflow.commerce.product.vo.ProductDetailVO;
import com.personaflow.commerce.product.vo.ProductListItemVO;
import com.personaflow.commerce.product.vo.SkuDetailVO;
import com.personaflow.commerce.product.vo.SkuVO;
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.CurrentUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/catalog")
public class ProductController {

    private static final String SOURCE_MODULE = "catalog";

    private final ProductService productService;
    private final CurrentUserProvider currentUserProvider;
    private final BehaviorEventPublishSupport behaviorEventPublishSupport;

    public ProductController(
            ProductService productService,
            CurrentUserProvider currentUserProvider,
            BehaviorEventPublishSupport behaviorEventPublishSupport
    ) {
        this.productService = productService;
        this.currentUserProvider = currentUserProvider;
        this.behaviorEventPublishSupport = behaviorEventPublishSupport;
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryVO>> listCategories() {
        return ApiResponse.success(productService.listCategoryTree());
    }

    @GetMapping("/products")
    public ApiResponse<PageResult<ProductListItemVO>> listProducts(
            @Positive @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @Min(1) @RequestParam(defaultValue = "1") Integer page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer size
    ) {
        PageResult<ProductListItemVO> result = productService.listProducts(categoryId, keyword, page, size);
        publishProductSearchIfAuthenticated(categoryId, keyword, page, size, result);
        return ApiResponse.success(result);
    }

    @GetMapping("/products/{spuId}")
    public ApiResponse<ProductDetailVO> getProductDetail(@Positive @PathVariable Long spuId) {
        ProductDetailVO productDetail = productService.getProductDetail(spuId);
        publishProductViewIfAuthenticated(productDetail);
        return ApiResponse.success(productDetail);
    }

    @GetMapping("/skus/{skuId}")
    public ApiResponse<SkuDetailVO> getSkuDetail(@Positive @PathVariable Long skuId) {
        return ApiResponse.success(productService.getSkuDetail(skuId));
    }

    private void publishProductViewIfAuthenticated(ProductDetailVO productDetail) {
        currentUserProvider.findCurrentUser()
                .ifPresent(currentUser -> behaviorEventPublishSupport.publish(productViewCommand(
                        currentUser,
                        productDetail
                )));
    }

    private void publishProductSearchIfAuthenticated(
            Long categoryId,
            String keyword,
            Integer page,
            Integer size,
            PageResult<ProductListItemVO> result
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return;
        }
        currentUserProvider.findCurrentUser()
                .ifPresent(currentUser -> behaviorEventPublishSupport.publish(productSearchCommand(
                        currentUser,
                        categoryId,
                        normalizedKeyword,
                        page,
                        size,
                        result
                )));
    }

    private BehaviorEventPublishCommand productViewCommand(
            CurrentUser currentUser,
            ProductDetailVO productDetail
    ) {
        SkuVO firstSku = productDetail.skus().isEmpty() ? null : productDetail.skus().get(0);
        return new BehaviorEventPublishCommand(
                BehaviorEventType.PRODUCT_VIEW,
                currentUser.userId(),
                SOURCE_MODULE,
                "SPU",
                productDetail.spuId(),
                null,
                firstSku == null ? null : firstSku.skuId(),
                productDetail.spuId(),
                productDetail.categoryId(),
                null,
                firstSku == null ? null : firstSku.price(),
                productDetailPayload(productDetail)
        );
    }

    private BehaviorEventPublishCommand productSearchCommand(
            CurrentUser currentUser,
            Long categoryId,
            String keyword,
            Integer page,
            Integer size,
            PageResult<ProductListItemVO> result
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "keyword", keyword);
        putIfPresent(payload, "categoryId", categoryId);
        putIfPresent(payload, "page", page);
        putIfPresent(payload, "size", size);
        putIfPresent(payload, "total", result.total());
        payload.put("records", result.records().stream()
                .map(this::productListItemPayload)
                .toList());

        return new BehaviorEventPublishCommand(
                BehaviorEventType.PRODUCT_SEARCH,
                currentUser.userId(),
                SOURCE_MODULE,
                "KEYWORD",
                null,
                keyword,
                null,
                null,
                categoryId,
                null,
                null,
                payload
        );
    }

    private Map<String, Object> productDetailPayload(ProductDetailVO productDetail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "spuId", productDetail.spuId());
        putIfPresent(payload, "categoryId", productDetail.categoryId());
        putIfPresent(payload, "categoryName", productDetail.categoryName());
        putIfPresent(payload, "productName", productDetail.name());
        putIfPresent(payload, "brand", productDetail.brand());
        putIfPresent(payload, "tags", productDetail.tags());
        putIfPresent(payload, "mainImageUrl", productDetail.mainImageUrl());
        payload.put("skus", productDetail.skus().stream()
                .map(this::skuPayload)
                .toList());
        return payload;
    }

    private Map<String, Object> productListItemPayload(ProductListItemVO item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "spuId", item.spuId());
        putIfPresent(payload, "categoryId", item.categoryId());
        putIfPresent(payload, "categoryName", item.categoryName());
        putIfPresent(payload, "productName", item.name());
        putIfPresent(payload, "brand", item.brand());
        putIfPresent(payload, "minPrice", item.minPrice());
        putIfPresent(payload, "maxPrice", item.maxPrice());
        putIfPresent(payload, "tags", item.tags());
        return payload;
    }

    private Map<String, Object> skuPayload(SkuVO sku) {
        Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "skuId", sku.skuId());
        putIfPresent(payload, "skuName", sku.skuName());
        putIfPresent(payload, "price", sku.price());
        putIfPresent(payload, "imageUrl", sku.imageUrl());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
