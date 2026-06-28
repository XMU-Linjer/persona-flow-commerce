package com.personaflow.commerce.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaflow.commerce.common.error.BusinessException;
import com.personaflow.commerce.common.error.ErrorCode;
import com.personaflow.commerce.common.vo.PageResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductCategoryMapper categoryMapper;

    @Mock
    private ProductSpuMapper spuMapper;

    @Mock
    private ProductSkuMapper skuMapper;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(categoryMapper, spuMapper, skuMapper, new ObjectMapper());
    }

    @Test
    void listCategoryTreeBuildsChildren() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "数码配件", null, 1, 10),
                category(101L, "耳机音频", 1L, 2, 11),
                category(102L, "充电存储", 1L, 2, 12)
        ));

        List<CategoryVO> categories = productService.listCategoryTree();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).id()).isEqualTo(1L);
        assertThat(categories.get(0).children())
                .extracting(CategoryVO::id)
                .containsExactly(101L, 102L);
    }

    @Test
    void listProductsReturnsPagedSkuAggregates() {
        ProductSpuEntity keyboard = spu(20001L, 201L, "KeyForge K3", "KeyForge", 1, 10);
        ProductSpuEntity mouse = spu(20002L, 201L, "SilentPro M8", "SilentPro", 1, 20);
        when(spuMapper.selectList(any())).thenReturn(List.of(keyboard, mouse));
        when(skuMapper.selectList(any())).thenReturn(List.of(
                sku(30001L, 20001L, "青轴 白色", "{}", "459.00", 100),
                sku(30002L, 20001L, "茶轴 黑色", "{}", "469.00", 80),
                sku(30003L, 20002L, "白色", "{}", "129.00", 200)
        ));
        when(categoryMapper.selectByIds(any())).thenReturn(List.of(category(201L, "键盘鼠标", 2L, 2, 21)));

        PageResult<ProductListItemVO> result = productService.listProducts(null, null, 1, 1);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.records()).hasSize(1);
        ProductListItemVO first = result.records().get(0);
        assertThat(first.spuId()).isEqualTo(20001L);
        assertThat(first.minPrice()).isEqualByComparingTo("459.00");
        assertThat(first.maxPrice()).isEqualByComparingTo("469.00");
        assertThat(first.salesCount()).isEqualTo(180);
    }

    @Test
    void getProductDetailParsesJsonFieldsAndReturnsSkus() {
        ProductSpuEntity spu = spu(20001L, 201L, "KeyForge K3", "KeyForge", 1, 10);
        spu.setDescription("Mechanical keyboard");
        spu.setDetailImagesJson("[\"detail-1.jpg\",\"detail-2.jpg\"]");
        spu.setAttributesJson("{\"连接方式\":\"蓝牙/2.4G/有线\",\"重量\":\"约 980g\"}");
        when(spuMapper.selectById(20001L)).thenReturn(spu);
        when(categoryMapper.selectById(201L)).thenReturn(category(201L, "键盘鼠标", 2L, 2, 21));
        when(skuMapper.selectList(any())).thenReturn(List.of(
                sku(30001L, 20001L, "青轴 白色", "{\"轴体\":\"青轴\"}", "459.00", 100)
        ));

        ProductDetailVO detail = productService.getProductDetail(20001L);

        assertThat(detail.spuId()).isEqualTo(20001L);
        assertThat(detail.detailImages()).containsExactly("detail-1.jpg", "detail-2.jpg");
        assertThat(detail.attributes()).containsEntry("重量", "约 980g");
        assertThat(detail.skus()).hasSize(1);
        assertThat(detail.skus().get(0).specs()).containsEntry("轴体", "青轴");
    }

    @Test
    void getProductDetailRejectsOffShelfSpu() {
        when(spuMapper.selectById(20001L))
                .thenReturn(spu(20001L, 201L, "KeyForge K3", "KeyForge", 0, 10));

        assertThatThrownBy(() -> productService.getProductDetail(20001L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CATALOG_PRODUCT_NOT_SELLABLE)
                );
    }

    @Test
    void getSkuDetailReturnsSellableSkuInformation() {
        when(skuMapper.selectById(30001L))
                .thenReturn(sku(30001L, 20001L, "青轴 白色", "{\"轴体\":\"青轴\"}", "459.00", 100));
        when(spuMapper.selectById(20001L)).thenReturn(spu(20001L, 201L, "KeyForge K3", "KeyForge", 1, 10));
        when(categoryMapper.selectById(201L)).thenReturn(category(201L, "键盘鼠标", 2L, 2, 21));

        SkuDetailVO detail = productService.getSkuDetail(30001L);

        assertThat(detail.skuId()).isEqualTo(30001L);
        assertThat(detail.productName()).isEqualTo("KeyForge K3");
        assertThat(detail.categoryName()).isEqualTo("键盘鼠标");
        assertThat(detail.specs()).containsEntry("轴体", "青轴");
    }

    @Test
    void getSkuDetailRejectsMissingSku() {
        when(skuMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> productService.getSkuDetail(999L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CATALOG_SKU_NOT_FOUND)
                );
    }

    private ProductCategoryEntity category(Long id, String name, Long parentId, Integer level, Integer sortOrder) {
        ProductCategoryEntity category = new ProductCategoryEntity();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSortOrder(sortOrder);
        category.setStatus(1);
        category.setIconUrl("icon-" + id + ".png");
        return category;
    }

    private ProductSpuEntity spu(Long id, Long categoryId, String name, String brand, Integer status, Integer sortOrder) {
        ProductSpuEntity spu = new ProductSpuEntity();
        spu.setId(id);
        spu.setCategoryId(categoryId);
        spu.setName(name);
        spu.setSubtitle(name + " subtitle");
        spu.setBrand(brand);
        spu.setMainImageUrl("spu-" + id + ".jpg");
        spu.setDetailImagesJson("[]");
        spu.setAttributesJson("{}");
        spu.setTags("办公,演示");
        spu.setStatus(status);
        spu.setSortOrder(sortOrder);
        return spu;
    }

    private ProductSkuEntity sku(Long id, Long spuId, String skuName, String specsJson, String price, Integer salesCount) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setSkuName(skuName);
        sku.setSpecsJson(specsJson);
        sku.setPrice(new BigDecimal(price));
        sku.setOriginalPrice(new BigDecimal(price).add(new BigDecimal("40.00")));
        sku.setImageUrl("sku-" + id + ".jpg");
        sku.setStatus(1);
        sku.setSalesCount(salesCount);
        return sku;
    }
}
