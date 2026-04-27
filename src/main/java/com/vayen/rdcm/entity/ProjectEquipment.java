package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目设备关联实体
 */
@Data
@TableName("project_equipment")
public class ProjectEquipment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("project_id")
    private Long projectId;

    @TableField("device_id")
    private Long deviceId;

    @TableField("linked_at")
    private LocalDateTime linkedAt;
}
