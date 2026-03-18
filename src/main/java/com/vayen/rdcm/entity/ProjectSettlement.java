package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目结算实体
 */
@Data
@TableName("project_settlement")
public class ProjectSettlement {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("amount")
    private Double amount;

    @TableField("settlement_date")
    private LocalDateTime settlementDate;

    @TableField("proof_files")
    private String proofFiles;

    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;
}