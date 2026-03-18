package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目人员关联实体
 */
@Data
@TableName("project_employee")
public class ProjectEmployee {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("role_in_project")
    private String roleInProject;

    @TableField("linked_at")
    private LocalDateTime linkedAt;
}