package com.vayen.rdcm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SettlementListItemResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String yearMonth;
    private String status;
    private Double amount;
    private LocalDateTime settledAt;
}
