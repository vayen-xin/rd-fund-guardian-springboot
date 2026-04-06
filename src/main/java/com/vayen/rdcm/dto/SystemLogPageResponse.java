package com.vayen.rdcm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SystemLogPageResponse {

    private List<SystemLogItem> records;
    private long current;
    private long size;
    private long total;

    @Data
    public static class SystemLogItem {
        private Long id;
        private Long companyId;
        private Long userId;
        private String operatorName;
        private String operatorUsername;
        private String module;
        private String action;
        private String details;
        private String status;
        private String resultMessage;
        private String ip;
        private String userAgent;
        private LocalDateTime createdAt;
    }
}
