package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目结算实体
 */
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }

    public BigDecimal getDirectInputCost() { return directInputCost; }
    public void setDirectInputCost(BigDecimal directInputCost) { this.directInputCost = directInputCost; }

    public BigDecimal getDepreciationCost() { return depreciationCost; }
    public void setDepreciationCost(BigDecimal depreciationCost) { this.depreciationCost = depreciationCost; }

    public BigDecimal getIntangibleAmortization() { return intangibleAmortization; }
    public void setIntangibleAmortization(BigDecimal intangibleAmortization) { this.intangibleAmortization = intangibleAmortization; }

    public BigDecimal getDesignTestCost() { return designTestCost; }
    public void setDesignTestCost(BigDecimal designTestCost) { this.designTestCost = designTestCost; }

    public BigDecimal getOutsourcingCost() { return outsourcingCost; }
    public void setOutsourcingCost(BigDecimal outsourcingCost) { this.outsourcingCost = outsourcingCost; }

    public BigDecimal getIpCost() { return ipCost; }
    public void setIpCost(BigDecimal ipCost) { this.ipCost = ipCost; }

    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal otherCost) { this.otherCost = otherCost; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public Long getSettledBy() { return settledBy; }
    public void setSettledBy(Long settledBy) { this.settledBy = settledBy; }

    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
}
