package com.personaflow.commerce.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("product_spu")
public class ProductSpuEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("category_id")
    private Long categoryId;

    @TableField("name")
    private String name;

    @TableField("subtitle")
    private String subtitle;

    @TableField("brand")
    private String brand;

    @TableField("description")
    private String description;

    @TableField("main_image_url")
    private String mainImageUrl;

    @TableField("detail_images_json")
    private String detailImagesJson;

    @TableField("attributes_json")
    private String attributesJson;

    @TableField("tags")
    private String tags;

    @TableField("status")
    private Integer status;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
