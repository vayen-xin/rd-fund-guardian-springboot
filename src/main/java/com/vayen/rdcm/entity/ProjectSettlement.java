package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目结算实体
 */
@Data
@Entity
@Table(name = "project_settlement")
public class ProjectSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    @Column(name = "labor_cost", precision = 15, scale = 2)
    private BigDecimal laborCost = BigDecimal.ZERO; // 人工费用

    @Column(name = "direct_input_cost", precision = 15, scale = 2)
    private BigDecimal directInputCost = BigDecimal.ZERO; // 直接投入费用

    @Column(name = "depreciation_cost", precision = 15, scale = 2)
    private BigDecimal depreciationCost = BigDecimal.ZERO; // 折旧费用

    @Column(name = "intangible_amortization", precision = 15, scale = 2)
    private BigDecimal intangibleAmortization = BigDecimal.ZERO; // 无形资产摊销

    @Column(name = "design_test_cost", precision = 15, scale = 2)
    private BigDecimal designTestCost = BigDecimal.ZERO; // 设计试验费用

    @Column(name = "outsourcing_cost", precision = 15, scale = 2)
    private BigDecimal outsourcingCost = BigDecimal.ZERO; // 外包合作费用

    @Column(name = "ip_cost", precision = 15, scale = 2)
    private BigDecimal ipCost = BigDecimal.ZERO; // 知识产权费用

    @Column(name = "other_cost", precision = 15, scale = 2)
    private BigDecimal otherCost = BigDecimal.ZERO; // 其他费用

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // 合计金额

    @Column(name = "settled_by", nullable = false)
    private Long settledBy; // 结算人 ID

    @Column(name = "settled_at", updatable = false)
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        settledAt = LocalDateTime.now();
    }
}
