package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.ComplianceAlertResponse;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ComplianceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页铃铛提醒的合规预警接口。
 */
@RestController
@RequestMapping("/api/v1/compliance-alerts")
@RequiredArgsConstructor
public class ComplianceAlertController {

    private final ComplianceAlertService complianceAlertService;
    private final CurrentUserService currentUserService;

    /**
     * 查询当前登录用户可见范围内的研发费用口径预警消息。
     */
    @GetMapping
    @SaCheckPermission("project:view")
    public Result<List<ComplianceAlertResponse>> listAlerts() {
        return Result.success(complianceAlertService.listAlerts(currentUserService.getCurrentUser()));
    }
}
