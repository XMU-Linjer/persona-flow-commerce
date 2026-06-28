package com.personaflow.commerce.favorite.controller;

import com.personaflow.commerce.common.api.ApiResponse;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.favorite.service.FavoriteService;
import com.personaflow.commerce.favorite.vo.FavoriteItemVO;
import com.personaflow.commerce.favorite.vo.FavoriteStatusVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/shopping/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{skuId}")
    public ApiResponse<FavoriteStatusVO> addFavorite(@Positive @PathVariable Long skuId) {
        return ApiResponse.success(favoriteService.addFavorite(skuId));
    }

    @DeleteMapping("/{skuId}")
    public ApiResponse<FavoriteStatusVO> removeFavorite(@Positive @PathVariable Long skuId) {
        return ApiResponse.success(favoriteService.removeFavorite(skuId));
    }

    @GetMapping
    public ApiResponse<PageResult<FavoriteItemVO>> listFavorites(
            @Min(1) @RequestParam(defaultValue = "1") Integer page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer size
    ) {
        return ApiResponse.success(favoriteService.listFavorites(page, size));
    }
}
