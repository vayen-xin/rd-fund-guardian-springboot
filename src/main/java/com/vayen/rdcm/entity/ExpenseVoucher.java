package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 费用凭证实体
 */
@Data
@TableName("expense_voucher")
public class ExpenseVoucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    @TableField("type")
    private String type;

    @TableField("amount")
    private Double amount;

    @TableField("description")
    private String description;

    @TableField("file_path")
    private String filePath;

    @TableField("create_time")
    private LocalDateTime createTime;
}