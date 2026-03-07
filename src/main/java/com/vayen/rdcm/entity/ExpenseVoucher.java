package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用凭证实体
 */
@Data
@Entity
@Table(name = "expense_voucher")
public class ExpenseVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "expense_type", nullable = false, length = 50)
    private String expenseType; // 八类费用类型

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "voucher_files", columnDefinition = "JSON")
    private String voucherFiles; // JSON 字符串：[{name, path, uploadTime}]

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
