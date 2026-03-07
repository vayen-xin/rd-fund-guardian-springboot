package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getOperationDetail() { return operationDetail; }
    public void setOperationDetail(String operationDetail) { this.operationDetail = operationDetail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
