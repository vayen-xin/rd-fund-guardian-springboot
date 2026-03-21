package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("username")
    private String username;

    @TableField("name")
    private String name;

    @TableField("role")
    private String role; // admin/branch_admin/employee

    @TableField("password_hash")
    private String passwordHash;

    @TableField("email")
    private String email;

    @TableField("phone")
    private String phone;

    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
