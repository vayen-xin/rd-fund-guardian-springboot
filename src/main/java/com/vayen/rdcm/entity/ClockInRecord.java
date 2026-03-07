package com.vayen.rdcm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 打卡记录实体
 */
@Entity
@Table(name = "clock_in_record")
public class ClockInRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId; // 逻辑关联，无外键

    @Column(name = "clock_in_time", nullable = false)
    private LocalDateTime clockInTime;

    @Column(name = "duration_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal durationHours; // 工时（小时）

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public LocalDateTime getClockInTime() { return clockInTime; }
    public void setClockInTime(LocalDateTime clockInTime) { this.clockInTime = clockInTime; }

    public BigDecimal getDurationHours() { return durationHours; }
    public void setDurationHours(BigDecimal durationHours) { this.durationHours = durationHours; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
