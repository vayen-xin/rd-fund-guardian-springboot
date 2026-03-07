package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 项目 - 员工关联实体
 */
@Data
@Entity
@Table(name = "project_employee")
public class ProjectEmployee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "coefficient", nullable = false, precision = 5, scale = 4)
    private BigDecimal coefficient; // 系数（如 0.7200 表示 72%）

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
