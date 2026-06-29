package com.personaflow.commerce.product.controller;

import com.personaflow.commerce.behavior.enums.BehaviorEventType;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishCommand;
import com.personaflow.commerce.behavior.messaging.BehaviorEventPublishSupport;
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
import com.personaflow.commerce.user.api.CurrentUserProvider;
import com.personaflow.commerce.user.api.model.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private BehaviorEventPublishSupport behaviorEventPublishSupport;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService, currentUserProvider, behaviorEventPublishSupport))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listCategoriesReturnsTree() throws Exception {
        when(productService.listCategoryTree()).thenReturn(List.of(
                new CategoryVO(1L, "Digital", null, 1, 10, "digital.png", List.of(
                        new CategoryVO(101L, "Audio", 1L, 2, 11, "audio.png", List.of())
                ))
        ));

        mockMvc.perform(get("/api/catalog/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].children[0].id").value(101));
    }

    @Test
    void loggedInKeywordSearchPublishesProductSearchEvent() throws Exception {
        when(productService.listProducts(201L, "keyboard", 2, 5)).thenReturn(new PageResult<>(
                List.of(productListItem()),
                2,
                5,
                12
        ));
        when(currentUserProvider.findCurrentUser()).thenReturn(Optional.of(currentUser()));

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
        ArgumentCaptor<BehaviorEventPublishCommand> commandCaptor =
                ArgumentCaptor.forClass(BehaviorEventPublishCommand.class);
        verify(behaviorEventPublishSupport).publish(commandCaptor.capture());
        BehaviorEventPublishCommand command = commandCaptor.getValue();
        assertThat(command.eventType()).isEqualTo(BehaviorEventType.PRODUCT_SEARCH);
        assertThat(command.userId()).isEqualTo(10001L);
        assertThat(command.sourceModule()).isEqualTo("catalog");
        assertThat(command.objectType()).isEqualTo("KEYWORD");
        assertThat(command.keyword()).isEqualTo("keyboard");
        assertThat(command.categoryId()).isEqualTo(201L);
    }

    @Test
    void loggedInProductDetailPublishesProductViewEvent() throws Exception {
        when(productService.getProductDetail(20001L)).thenReturn(productDetail());
        when(currentUserProvider.findCurrentUser()).thenReturn(Optional.of(currentUser()));

        mockMvc.perform(get("/api/catalog/products/20001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.spuId").value(20001))
                .andExpect(jsonPath("$.data.detailImages[0]").value("detail-1.jpg"))
                .andExpect(jsonPath("$.data.attributes.connection").value("bluetooth"))
                .andExpect(jsonPath("$.data.skus[0].skuId").value(30001));

        ArgumentCaptor<BehaviorEventPublishCommand> commandCaptor =
                ArgumentCaptor.forClass(BehaviorEventPublishCommand.class);
        verify(behaviorEventPublishSupport).publish(commandCaptor.capture());
        BehaviorEventPublishCommand command = commandCaptor.getValue();
        assertThat(command.eventType()).isEqualTo(BehaviorEventType.PRODUCT_VIEW);
        assertThat(command.userId()).isEqualTo(10001L);
        assertThat(command.sourceModule()).isEqualTo("catalog");
        assertThat(command.objectType()).isEqualTo("SPU");
        assertThat(command.objectId()).isEqualTo(20001L);
        assertThat(command.spuId()).isEqualTo(20001L);
        assertThat(command.skuId()).isEqualTo(30001L);
        assertThat(command.amount()).isEqualByComparingTo("459.00");
    }

    @Test
    void anonymousProductDetailDoesNotPublishBehaviorEventAndStillSucceeds() throws Exception {
        when(productService.getProductDetail(20001L)).thenReturn(productDetail());
        when(currentUserProvider.findCurrentUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/products/20001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.spuId").value(20001));

        verify(behaviorEventPublishSupport, never()).publish(any());
    }

    @Test
    void getSkuDetailReturnsSkuInformation() throws Exception {
        when(productService.getSkuDetail(30001L)).thenReturn(new SkuDetailVO(
                30001L,
                20001L,
                "KeyForge K3",
                "White",
                201L,
                "Keyboard Mouse",
                new BigDecimal("459.00"),
                new BigDecimal("529.00"),
                "sku.jpg",
                Map.of("switch", "blue"),
                1
        ));

        mockMvc.perform(get("/api/catalog/skus/30001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.skuId").value(30001))
                .andExpect(jsonPath("$.data.productName").value("KeyForge K3"))
                .andExpect(jsonPath("$.data.specs.switch").value("blue"));
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
                "Keyboard Mouse",
                "KeyForge K3",
                "Tri-mode mechanical keyboard",
                "KeyForge",
                "main.jpg",
                new BigDecimal("459.00"),
                new BigDecimal("469.00"),
                180,
                "keyboard,office",
                1
        );
    }

    private ProductDetailVO productDetail() {
        return new ProductDetailVO(
                20001L,
                201L,
                "Keyboard Mouse",
                "KeyForge K3",
                "Tri-mode mechanical keyboard",
                "KeyForge",
                "Mechanical keyboard",
                "main.jpg",
                List.of("detail-1.jpg"),
                Map.of("connection", "bluetooth"),
                "keyboard,office",
                List.of(new SkuVO(
                        30001L,
                        "White",
                        Map.of("switch", "blue"),
                        new BigDecimal("459.00"),
                        new BigDecimal("529.00"),
                        "sku.jpg",
                        1,
                        100
                ))
        );
    }

    private CurrentUser currentUser() {
        return new CurrentUser(10001L, Set.of("ROLE_USER"));
    }
}
