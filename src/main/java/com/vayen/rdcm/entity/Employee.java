package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 员工实体
 */
@Data
@TableName("employee")
public class Employee {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("branch_id")
    private Long branchId;

    @TableField("employee_id")
    private String employeeId;

    @TableField("name")
    private String name;

    @TableField("gender")
    private String gender;

    @TableField("phone")
    private String phone;

    @TableField("position")
    private String position;

    @TableField("employee_type")
    private String employeeType;

    @TableField("entry_date")
    private String entryDate;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}