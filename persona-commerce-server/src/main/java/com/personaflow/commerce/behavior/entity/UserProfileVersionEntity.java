package com.personaflow.commerce.behavior.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("user_profile_version")
public class UserProfileVersionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("profile_json")
    private String profileJson;

    @TableField("summary")
    private String summary;

    @TableField("source_workflow_id")
    private String sourceWorkflowId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
