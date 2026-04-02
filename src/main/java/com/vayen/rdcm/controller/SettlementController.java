package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.entity.ProjectSettlement;
import com.vayen.rdcm.mapper.ProjectSettlementMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.service.ProjectService;
import com.vayen.rdcm.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    /**
     * 创建项目结算
     */
    @PostMapping("/{projectId}/create")
    @SaCheckPermission("settlement:create")
    public Result<ProjectSettlement> createSettlement(
            @PathVariable Long projectId,
            @RequestParam String settlementMonth) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        projectService.assertProjectAccess(projectId, currentUser);
        ProjectSettlement settlement = settlementService.createSettlement(projectId, parseMonth(settlementMonth), currentUser.getId());
        return Result.success(settlement);
    }

    /**
     * 查询项目结算记录
     */
    @GetMapping("/{projectId}")
    @SaCheckPermission("settlement:view")
    public Result<List<ProjectSettlement>> getProjectSettlements(
            @PathVariable Long projectId,
            @RequestParam(required = false) String month) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        List<ProjectSettlement> list;

        if (month != null && !month.isBlank()) {
            LocalDateTime monthStart = parseMonth(month).atTime(0, 0, 0);
            QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
            wrapper.eq("project_id", projectId)
                    .eq("settlement_month", monthStart)
                    .orderByDesc("settlement_month");
            list = settlementMapper.selectList(wrapper);
        } else {
            QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
            wrapper.eq("project_id", projectId)
                    .orderByDesc("settlement_month");
            list = settlementMapper.selectList(wrapper);
        }

        return Result.success(list);
    }

    /**
     * 重新结算
     */
    @PostMapping("/{projectId}/re-settle")
    @SaCheckPermission("settlement:edit")
    public Result<ProjectSettlement> reSettle(
            @PathVariable Long projectId,
            @RequestParam String settlementMonth) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        projectService.assertProjectAccess(projectId, currentUser);
        ProjectSettlement settlement = settlementService.reSettle(projectId, parseMonth(settlementMonth), currentUser.getId());
        return Result.success(settlement);
    }

    private LocalDate parseMonth(String month) {
        return YearMonth.parse(month).atDay(1);
    }
}
