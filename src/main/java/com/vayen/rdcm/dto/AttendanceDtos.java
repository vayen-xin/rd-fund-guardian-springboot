package com.vayen.rdcm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AttendanceDtos {

    @Data
    public static class AttendanceListItem {
        private Long id;
        private String employeeId;
        private String name;
        private String projectCode;
        private String projectName;
        private LocalDate date;
        private BigDecimal duration;
        private String source;
    }

    @Data
    public static class AttendanceImportRow {
        private String employeeId;
        private String name;
        private String projectCode;
        private String projectName;
        private LocalDate date;
        private BigDecimal duration;
        private String source;
    }

    @Data
    public static class AttendanceImportPreviewResponse {
        private int successCount;
        private int failedCount;
        private List<AttendanceImportRow> rows;
    }

    @Data
    public static class AttendanceImportConfirmRequest {
        private List<AttendanceImportRow> rows;
    }

    @Data
    public static class AttendanceImportPreviewRequest {
        private String month;
    }

    @Data
    public static class AttendanceSaveRequest {
        private String employeeId;
        private String name;
        private String projectCode;
        private LocalDate date;
        private BigDecimal duration;
    }

    @Data
    public static class AttendanceLookupResponse {
        private String employeeId;
        private String name;
        private String department;
        private boolean exactMatch;
        private String hint;
    }
}
