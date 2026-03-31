package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司实体
 */
@Data
@TableName("company")
public class Company {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("status")
    private String status = "active";

    // @TableField("created_at")
    @TableField(value = "created_at",
            insertStrategy = FieldStrategy.NEVER, // 插入时永不填充此字段，让数据库处理
            updateStrategy = FieldStrategy.NEVER  // 更新时永不填充此字段
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // @TableField("updated_at")
    @TableField(value = "updated_at",
            insertStrategy = FieldStrategy.NEVER, // 插入时永不填充此字段，让数据库处理
            updateStrategy = FieldStrategy.NEVER  // 更新时永不填充此字段，让数据库处理
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
