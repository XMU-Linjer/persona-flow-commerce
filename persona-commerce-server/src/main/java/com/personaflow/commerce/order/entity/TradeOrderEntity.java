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
@TableName("trade_order")
public class TradeOrderEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("address_id")
    private Long addressId;

    @TableField("recipient_name")
    private String recipientName;

    @TableField("recipient_phone")
    private String recipientPhone;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("postal_code")
    private String postalCode;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("status")
    private Integer status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("canceled_at")
    private LocalDateTime canceledAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
