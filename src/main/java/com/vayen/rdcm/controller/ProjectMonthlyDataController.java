package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.vayen.rdcm.audit.AuditLog;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.MonthlyDataDetailResponse;
import com.vayen.rdcm.dto.MonthlyFeeSchemaResponse;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ProjectMonthlyDataService;
import com.vayen.rdcm.service.ProjectService;
import com.vayen.rdcm.util.MonthlyFeeCatalog;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 项目月度费用接口。
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectMonthlyDataController {

    private final ProjectMonthlyDataService monthlyDataService;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public ProjectMonthlyDataController(ProjectMonthlyDataService monthlyDataService,
                                        ProjectService projectService,
                                        CurrentUserService currentUserService) {
        this.monthlyDataService = monthlyDataService;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    /**
     * 获取项目某月数据
     */
    @GetMapping("/{projectId}/monthly/{month}")
    @SaCheckPermission("project:view")
    public Result<MonthlyDataDetailResponse> getMonthlyData(
            @PathVariable Long projectId,
            @PathVariable String month) {
        LocalDate workMonth = parseMonth(month);
        return Result.success(projectService.getMonthlyDetail(projectId, workMonth, currentUserService.getCurrentUser()));
    }

    /**
     * 获取固定的月度费用分类目录
     */
    @GetMapping("/monthly-fee-schema")
    @SaCheckPermission("project:view")
    public Result<MonthlyFeeSchemaResponse> getMonthlyFeeSchema() {
        return Result.success(MonthlyFeeCatalog.buildResponse());
    }

    /**
     * 创建或更新月度数据
     */
    @PutMapping("/{projectId}/monthly/{month}")
    @SaCheckPermission("project:edit")
    @AuditLog(module = "月度费用", action = "保存月度数据")
    public Result<Void> saveMonthlyData(
            @PathVariable Long projectId,
            @PathVariable String month,
            @RequestBody MonthlyDataSaveRequest request) {
        LocalDate workMonth = parseMonth(month);
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        monthlyDataService.saveMonthlyData(
                projectId,
                workMonth,
                request.getCostData(),
                request.getEmployeeData(),
                request.getDeviceData(),
                request.getGrandTotal(),
                currentUserService.getCurrentUser()
        );
        return Result.success();
    }

    /**
     * 获取项目所有月度数据列表
     */
    @GetMapping("/{projectId}/monthly")
    @SaCheckPermission("project:view")
    public Result<List<ProjectMonthlyData>> getProjectMonthlyList(@PathVariable Long projectId) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        return Result.success(monthlyDataService.getProjectMonthlyList(projectId));
    }

    /**
     * 提交月度数据
     */
    @PostMapping("/{projectId}/monthly/{month}/submit")
    @SaCheckPermission("project:edit")
    @AuditLog(module = "月度费用", action = "提交月度数据")
    public Result<Void> submitMonthlyData(
            @PathVariable Long projectId,
            @PathVariable String month) {
        LocalDate workMonth = parseMonth(month);
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        monthlyDataService.submitMonthlyData(projectId, workMonth, currentUserService.getCurrentUserId());
        return Result.success();
    }

    /**
     * 手动触发月度数据继承
     */
    @PostMapping("/{projectId}/monthly/inherit")
    @SaCheckRole("admin")
    @AuditLog(module = "月度费用", action = "继承月度数据")
    public Result<Void> triggerInherit(
            @PathVariable Long projectId,
            @RequestBody InheritRequest request) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        monthlyDataService.inheritFromLastMonth(
                projectId,
                parseMonth(request.getFromMonth()),
                parseMonth(request.getToMonth()),
                currentUserService.getCurrentUserId()
        );
        return Result.success();
    }

    private LocalDate parseMonth(String month) {
        return YearMonth.parse(month).atDay(1);
    }
}

class MonthlyDataSaveRequest {
    private String costData;
    private String employeeData;
    private String deviceData;
    private Double grandTotal;

    public String getCostData() { return costData; }
    public void setCostData(String costData) { this.costData = costData; }
    public String getEmployeeData() { return employeeData; }
    public void setEmployeeData(String employeeData) { this.employeeData = employeeData; }
    public String getDeviceData() { return deviceData; }
    public void setDeviceData(String deviceData) { this.deviceData = deviceData; }
    public Double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
}

class InheritRequest {
    private String fromMonth;
    private String toMonth;

    public String getFromMonth() { return fromMonth; }
    public void setFromMonth(String fromMonth) { this.fromMonth = fromMonth; }
    public String getToMonth() { return toMonth; }
    public void setToMonth(String toMonth) { this.toMonth = toMonth; }
}
