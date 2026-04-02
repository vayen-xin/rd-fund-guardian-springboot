package com.vayen.rdcm.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectDetailResponse {
    private Long id;
    private String projectName;
    private String code;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long companyId;
    private String managerName;
    private String managerPhone;
    private Integer employeeCount;
    private Integer deviceCount;
    private Double settlementAmount;
    private List<ProjectEmployeeItem> employees;
    private List<ProjectDeviceItem> devices;
    private List<ProjectLogItem> logs;

    @Data
    public static class ProjectEmployeeItem {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String employeeType;
        private Double coefficient;
        private String department;
        private String phone;
        private String email;
    }

    @Data
    public static class ProjectDeviceItem {
        private Long id;
        private Long deviceId;
        private String deviceName;
        private String model;
        private String status;
        private Double dailyDepreciation;
        private Double monthlyRental;
    }

    @Data
    public static class ProjectLogItem {
        private Long id;
        private String action;
        private String targetType;
        private Long targetId;
        private String remark;
        private Long operatorId;
        private LocalDateTime createdAt;
    }
}
