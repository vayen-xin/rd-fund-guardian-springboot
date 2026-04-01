package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat; // 引入注解
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备实体
 */
@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("device_name")
    private String deviceName;

    @TableField("model")
    private String model;

    @TableField("specification")
    private String specification;

    @TableField("purchase_date")
    private LocalDate purchaseDate;

    @TableField("purchase_price")
    private Double purchasePrice;

    @TableField("daily_depreciation")
    private Double dailyDepreciation;

    @TableField("monthly_rental")
    private Double monthlyRental;

    @TableField("status")
    private String status; // normal/maintenance/scrapped

    @TableField("notes")
    private String notes;

    @TableField("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 用于处理JSON输入/输出格式
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") // 用于处理JSON输入/输出格式
    private LocalDateTime updatedAt;
}