package com.personaflow.commerce.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user_role")
public class UserRoleEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;
}
