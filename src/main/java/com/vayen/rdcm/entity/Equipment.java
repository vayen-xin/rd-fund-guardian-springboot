package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备库实体
 */
@Data
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "depreciation_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal depreciationRate; // 折旧单价（元/小时）

    @Column(name = "status", columnDefinition = "INT DEFAULT 1")
    private Integer status = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
