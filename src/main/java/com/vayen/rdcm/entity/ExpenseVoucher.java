package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("expense_voucher")
public class ExpenseVoucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("project_id")
    private Long projectId;

    @TableField("year_month")
    private String yearMonth;

    @TableField("category")
    private String category;

    @TableField("original_file_name")
    private String originalFileName;

    @TableField("stored_file_name")
    private String storedFileName;

    @TableField("relative_path")
    private String relativePath;

    @TableField("content_type")
    private String contentType;

    @TableField("file_size")
    private Long fileSize;

    @TableField("uploaded_by")
    private Long uploadedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
