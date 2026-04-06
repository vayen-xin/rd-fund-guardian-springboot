package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat; // 引入注解
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

    @TableField("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 用于处理JSON输入/输出格式
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 用于处理JSON输入/输出格式
    private LocalDateTime updatedAt;
}