package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType; // 操作类型

    @Column(name = "operation_detail", columnDefinition = "JSON")
    private String operationDetail; // JSON 字符串：动态内容

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
