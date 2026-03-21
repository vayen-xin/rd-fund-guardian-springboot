package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目员工关联实体
 */
@Data
@TableName("project_employee")
public class ProjectEmployee {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("project_id")
    private Long projectId;

    @TableField("employee_id")
    private Long employeeId; // 关联 employee 表的 ID

    @TableField("employee_name")
    private String employeeName;

    @TableField("employee_type")
    private String employeeType; // formal/part_time

    @TableField("coefficient")
    private Double coefficient; // 默认系数，月度费用可覆盖

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("department")
    private String department;

    @TableField("notes")
    private String notes;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
