package com.personaflow.commerce.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_login_identity")
public class LoginIdentityEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("identity_type")
    private String identityType;

    @TableField("identifier")
    private String identifier;

    @TableField("normalized_identifier")
    private String normalizedIdentifier;

    @TableField("verified")
    private Boolean verified;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
