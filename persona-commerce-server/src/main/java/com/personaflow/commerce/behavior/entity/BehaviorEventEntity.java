package com.personaflow.commerce.behavior.entity;

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
@TableName("behavior_event")
public class BehaviorEventEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("event_id")
    private String eventId;

    @TableField("user_id")
    private Long userId;

    @TableField("event_type")
    private String eventType;

    @TableField("source_module")
    private String sourceModule;

    @TableField("object_type")
    private String objectType;

    @TableField("object_id")
    private Long objectId;

    @TableField("keyword")
    private String keyword;

    @TableField("sku_id")
    private Long skuId;

    @TableField("spu_id")
    private Long spuId;

    @TableField("category_id")
    private Long categoryId;

    @TableField("order_id")
    private Long orderId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
