package com.personaflow.commerce.product.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.product.service.ProductService;
import com.personaflow.commerce.product.vo.CategoryVO;
import com.personaflow.commerce.product.vo.ProductDetailVO;
import com.personaflow.commerce.product.vo.ProductListItemVO;
import com.personaflow.commerce.product.vo.SkuDetailVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/catalog")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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
        return ApiResponse.success(productService.listProducts(categoryId, keyword, page, size));
    }

    @GetMapping("/products/{spuId}")
    public ApiResponse<ProductDetailVO> getProductDetail(@Positive @PathVariable Long spuId) {
        return ApiResponse.success(productService.getProductDetail(spuId));
    }

    @GetMapping("/skus/{skuId}")
    public ApiResponse<SkuDetailVO> getSkuDetail(@Positive @PathVariable Long skuId) {
        return ApiResponse.success(productService.getSkuDetail(skuId));
    }
}
