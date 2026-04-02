package com.vayen.rdcm.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MonthlyDataDetailResponse {
    private Long projectId;
    private String yearMonth;
    private String status;
    private String settledAt;
    private Double grandTotal;
    private List<EmployeeItem> employees;
    private List<DeviceItem> devices;
    private Map<String, Object> fees;

    @Data
    public static class EmployeeItem {
        private Long employeeId;
        private String employeeNo;
        private String name;
        private String department;
        private String employeeType;
        private Double coefficient;
        private Double hourlyRate;
    }

    @Data
    public static class DeviceItem {
        private Long deviceId;
        private String deviceNo;
        private String name;
        private String category;
        private Double depreciationRate;
        private Boolean isUsed;
    }
}
