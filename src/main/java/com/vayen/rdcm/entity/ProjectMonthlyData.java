package com.vayen.rdcm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目月度费用数据实体（JSON存储）
 */
@Data
@TableName("project_monthly_data")
public class ProjectMonthlyData {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("company_id")
    private Long companyId;

    @TableField("project_id")
    private Long projectId;

    @TableField("work_month")
    private LocalDateTime workMonth; // 格式：YYYY-MM-01 00:00:00

    @TableField("cost_data")
    private String costData; // JSON格式

    // 冗余字段
    @TableField("labor_total")
    private Double laborTotal;
    @TableField("direct_material_total")
    private Double directMaterialTotal;
    @TableField("direct_fuel_total")
    private Double directFuelTotal;
    @TableField("direct_rental_total")
    private Double directRentalTotal;
    @TableField("depreciation_total")
    private Double depreciationTotal;
    @TableField("amortization_total")
    private Double amortizationTotal;
    @TableField("design_total")
    private Double designTotal;
    @TableField("commissioning_total")
    private Double commissioningTotal;
    @TableField("outsourced_total")
    private Double outsourcedTotal;
    @TableField("other_total")
    private Double otherTotal;
    @TableField("grand_total")
    private Double grandTotal;

    @TableField("version")
    private Integer version;

    @TableField("status")
    private String status;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
