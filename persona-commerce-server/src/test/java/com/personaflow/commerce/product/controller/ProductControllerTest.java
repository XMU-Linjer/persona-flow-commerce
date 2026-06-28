package com.personaflow.commerce.product.controller;

import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.error.GlobalExceptionHandler;
import com.personaflow.commerce.common.vo.PageResult;
import com.personaflow.commerce.product.service.ProductService;
import com.personaflow.commerce.product.vo.CategoryVO;
import com.personaflow.commerce.product.vo.ProductDetailVO;
import com.personaflow.commerce.product.vo.ProductListItemVO;
import com.personaflow.commerce.product.vo.SkuDetailVO;
import com.personaflow.commerce.product.vo.SkuVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listCategoriesReturnsTree() throws Exception {
        when(productService.listCategoryTree()).thenReturn(List.of(
                new CategoryVO(1L, "数码配件", null, 1, 10, "digital.png", List.of(
                        new CategoryVO(101L, "耳机音频", 1L, 2, 11, "audio.png", List.of())
                ))
        ));

        mockMvc.perform(get("/api/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].children[0].id").value(101));
    }

    @Test
    void listProductsReturnsPagedResponseAndPassesQueryParams() throws Exception {
        when(productService.listProducts(201L, "keyboard", 2, 5)).thenReturn(new PageResult<>(
                List.of(productListItem()),
                2,
                5,
                12
        ));

        mockMvc.perform(get("/api/catalog/products")
                        .param("categoryId", "201")
                        .param("keyword", "keyboard")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.records[0].spuId").value(20001))
                .andExpect(jsonPath("$.data.records[0].minPrice").value(459.00));

        verify(productService).listProducts(201L, "keyboard", 2, 5);
    }

    @Test
    void getProductDetailReturnsSkuList() throws Exception {
        when(productService.getProductDetail(20001L)).thenReturn(new ProductDetailVO(
                20001L,
                201L,
                "键盘鼠标",
                "KeyForge K3",
                "三模机械键盘",
                "KeyForge",
                "Mechanical keyboard",
                "main.jpg",
                List.of("detail-1.jpg"),
                Map.of("连接方式", "蓝牙/2.4G/有线"),
                "机械键盘,办公",
                List.of(new SkuVO(
                        30001L,
                        "青轴 白色",
                        Map.of("轴体", "青轴"),
                        new BigDecimal("459.00"),
                        new BigDecimal("529.00"),
                        "sku.jpg",
                        1,
                        100
                ))
        ));

        mockMvc.perform(get("/api/catalog/products/20001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.spuId").value(20001))
                .andExpect(jsonPath("$.data.detailImages[0]").value("detail-1.jpg"))
                .andExpect(jsonPath("$.data.attributes.连接方式").value("蓝牙/2.4G/有线"))
                .andExpect(jsonPath("$.data.skus[0].skuId").value(30001));
    }

    @Test
    void getSkuDetailReturnsSkuInformation() throws Exception {
        when(productService.getSkuDetail(30001L)).thenReturn(new SkuDetailVO(
                30001L,
                20001L,
                "KeyForge K3",
                "青轴 白色",
                201L,
                "键盘鼠标",
                new BigDecimal("459.00"),
                new BigDecimal("529.00"),
                "sku.jpg",
                Map.of("轴体", "青轴"),
                1
        ));

        mockMvc.perform(get("/api/catalog/skus/30001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(30001))
                .andExpect(jsonPath("$.data.productName").value("KeyForge K3"))
                .andExpect(jsonPath("$.data.specs.轴体").value("青轴"));
    }

    @Test
    void missingSkuReturnsBusinessError() throws Exception {
        when(productService.getSkuDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.CATALOG_SKU_NOT_FOUND));

        mockMvc.perform(get("/api/catalog/skus/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("CATALOG_SKU_NOT_FOUND"));
    }

    private ProductListItemVO productListItem() {
        return new ProductListItemVO(
                20001L,
                201L,
                "键盘鼠标",
                "KeyForge K3",
                "三模机械键盘",
                "KeyForge",
                "main.jpg",
                new BigDecimal("459.00"),
                new BigDecimal("469.00"),
                180,
                "机械键盘,办公",
                1
        );
    }
}
