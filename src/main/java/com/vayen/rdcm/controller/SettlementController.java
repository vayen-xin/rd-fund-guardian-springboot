package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectSettlement;
import com.vayen.rdcm.mapper.ProjectSettlementMapper;
import com.vayen.rdcm.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目结算控制器
 */
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final ProjectSettlementMapper settlementMapper;
    
    /**
     * 创建项目结算
     */
    @PostMapping("/{projectId}/create")
    @SaCheckPermission("settlement:create")
    public Result<ProjectSettlement> createSettlement(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate settlementMonth) {

        Long currentUserId = StpUtil.getLoginIdAsLong();
        ProjectSettlement settlement = settlementService.createSettlement(projectId, settlementMonth, currentUserId);
        return Result.success(settlement);
    }
    
    /**
     * 查询项目结算记录
     */
    @GetMapping("/{projectId}")
    @SaCheckPermission("settlement:view")
    public Result<List<ProjectSettlement>> getProjectSettlements(
            @PathVariable Long projectId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") LocalDate month) {
        
        List<ProjectSettlement> list;
        
        if (month != null) {
            // 查询特定月份
            LocalDateTime monthStart = month.atTime(0, 0, 0);
            QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
            wrapper.eq("project_id", projectId)
                   .eq("settlement_month", monthStart)
                   .orderByDesc("settlement_month");
            list = settlementMapper.selectList(wrapper);
        } else {
            // 查询所有月份
            QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
            wrapper.eq("project_id", projectId)
                   .orderByDesc("settlement_month");
            list = settlementMapper.selectList(wrapper);
        }
        
        return Result.success(list);
    }
    
    /**
     * 重新结算（审批驳回后）
     */
    @PostMapping("/{projectId}/re-settle")
    @SaCheckPermission("settlement:edit")
    public Result<ProjectSettlement> reSettle(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") LocalDate settlementMonth) {
        
        Long currentUserId = StpUtil.getLoginIdAsLong();
        ProjectSettlement settlement = settlementService.reSettle(projectId, settlementMonth, currentUserId);
        return Result.success(settlement);
    }
}
