package com.personaflow.commerce.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("trade_order_item")
public class TradeOrderItemEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("sku_id")
    private Long skuId;

    @TableField("spu_id")
    private Long spuId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("category_name")
    private String categoryName;

    @TableField("product_name")
    private String productName;

    @TableField("sku_name")
    private String skuName;

    @TableField("image_url")
    private String imageUrl;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("quantity")
    private Integer quantity;

    @TableField("subtotal")
    private BigDecimal subtotal;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
