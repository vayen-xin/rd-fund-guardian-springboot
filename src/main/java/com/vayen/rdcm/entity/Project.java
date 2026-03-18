package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目实体
 */
@Data
@TableName("project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("branch_id")
    private Long branchId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("start_date")
    private String startDate;

    @TableField("end_date")
    private String endDate;

    @TableField("status")
    private Integer status;

    @TableField("amount")
    private Double amount;

    @TableField("settlement_proof")
    private String settlementProof;

    @TableField("create_user")
    private Long createUser;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("settle_time")
    private LocalDateTime settleTime;
}