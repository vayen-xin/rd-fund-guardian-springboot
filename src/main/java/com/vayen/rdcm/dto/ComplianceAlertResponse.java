package com.vayen.rdcm.dto;

import lombok.Data;

/**
 * 首页铃铛提醒使用的合规预警消息。
 */
@Data
public class ComplianceAlertResponse {

    private Long projectId;

    private String projectName;

    private String scopeCode;

    private String scopeLabel;

    private String message;

    private String workMonth;
}
