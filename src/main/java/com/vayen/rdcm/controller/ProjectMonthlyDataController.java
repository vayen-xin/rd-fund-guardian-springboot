package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.service.ProjectMonthlyDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目月度费用数据控制器
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectMonthlyDataController {

    private final ProjectMonthlyDataService monthlyDataService;

    public ProjectMonthlyDataController(ProjectMonthlyDataService monthlyDataService) {
        this.monthlyDataService = monthlyDataService;
    }
    
    /**
     * 获取项目某月数据
     */
    @GetMapping("/{projectId}/monthly/{month}")
    @SaCheckPermission("project:view")
    public Result<ProjectMonthlyData> getMonthlyData(
            @PathVariable Long projectId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        ProjectMonthlyData data = monthlyDataService.getProjectMonthlyData(projectId, month);
        return Result.success(data);
    }
    
    /**
     * 创建或更新月度数据
     */
    @PutMapping("/{projectId}/monthly/{month}")
    @SaCheckPermission("project:edit")
    public Result<Void> saveMonthlyData(
            @PathVariable Long projectId,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") LocalDate month,
            @RequestBody MonthlyDataSaveRequest request) {
        
        Long currentUserId = StpUtil.getLoginIdAsLong();
        monthlyDataService.saveMonthlyData(
            projectId, 
            month, 
            request.getCostData(), 
            request.getGrandTotal(),
            currentUserId
        );
        return Result.success();
    }
    
    /**
     * 获取项目所有月度数据列表
     */
    @GetMapping("/{projectId}/monthly")
    @SaCheckPermission("project:view")
    public Result<List<ProjectMonthlyData>> getProjectMonthlyList(@PathVariable Long projectId) {
        List<ProjectMonthlyData> list = monthlyDataService.getProjectMonthlyList(projectId);
        return Result.success(list);
    }
    
    /**
     * 手动触发月度数据继承（管理员功能）
     */
    @PostMapping("/{projectId}/monthly/inherit")
    @SaCheckRole("admin") // 仅管理员可手动触发
    public Result<Void> triggerInherit(
            @PathVariable Long projectId,
            @RequestBody InheritRequest request) {
        
        Long currentUserId = StpUtil.getLoginIdAsLong();
        monthlyDataService.inheritFromLastMonth(
            projectId,
            request.getFromMonth(),
            request.getToMonth(),
            currentUserId
        );
        return Result.success();
    }
}

/**
 * 保存月度数据请求体
 */
class MonthlyDataSaveRequest {
    private String costData;
    private Double grandTotal;
    
    // getters & setters
    public String getCostData() { return costData; }
    public void setCostData(String costData) { this.costData = costData; }
    public Double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
}

/**
 * 继承请求体
 */
class InheritRequest {
    private LocalDate fromMonth;
    private LocalDate toMonth;
    
    // getters & setters
    public LocalDate getFromMonth() { return fromMonth; }
    public void setFromMonth(LocalDate fromMonth) { this.fromMonth = fromMonth; }
    public LocalDate getToMonth() { return toMonth; }
    public void setToMonth(LocalDate toMonth) { this.toMonth = toMonth; }
}
