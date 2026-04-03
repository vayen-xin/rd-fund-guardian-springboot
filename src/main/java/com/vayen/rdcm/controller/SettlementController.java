package com.vayen.rdcm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.common.Result;
import com.vayen.rdcm.dto.PageResponse;
import com.vayen.rdcm.dto.SettlementListItemResponse;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectSettlement;
import com.vayen.rdcm.mapper.ProjectSettlementMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.CurrentUserService;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.ProjectService;
import com.vayen.rdcm.service.SettlementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 项目结算控制器
 */
@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SettlementService settlementService;
    private final ProjectSettlementMapper settlementMapper;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    /**
     * 获取结算列表
     */
    @GetMapping
    @SaCheckPermission("settlement:view")
    public Result<PageResponse<SettlementListItemResponse>> getSettlements(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String yearMonth) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status);
        }
        if (yearMonth != null && !yearMonth.isBlank()) {
            wrapper.eq("settlement_month", parseMonth(yearMonth).atTime(0, 0, 0));
        }
        wrapper.orderByDesc("settlement_month").orderByDesc("created_at");

        List<ProjectSettlement> all = settlementMapper.selectList(wrapper);
        long total = all.size();
        int fromIndex = Math.max(0, (page - 1) * size);
        int toIndex = Math.min(all.size(), fromIndex + size);
        List<SettlementListItemResponse> list = all.subList(fromIndex, toIndex).stream()
                .map(this::toSettlementListItem)
                .toList();
        return Result.success(new PageResponse<>(list, page.longValue(), size.longValue(), total));
    }

    /**
     * 获取项目结算记录
     */
    @GetMapping("/{projectId}")
    @SaCheckPermission("settlement:view")
    public Result<List<ProjectSettlement>> getProjectSettlements(
            @PathVariable Long projectId,
            @RequestParam(required = false) String month) {
        projectService.assertProjectAccess(projectId, currentUserService.getCurrentUser());
        QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        if (month != null && !month.isBlank()) {
            wrapper.eq("settlement_month", parseMonth(month).atTime(0, 0, 0));
        }
        wrapper.orderByDesc("settlement_month");
        return Result.success(settlementMapper.selectList(wrapper));
    }

    /**
     * 确认结算
     */
    @PostMapping("/{projectId}/confirm")
    @SaCheckPermission("settlement:create")
    public Result<ProjectSettlement> confirmSettlement(
            @PathVariable Long projectId,
            @RequestBody SettlementActionRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        projectService.assertProjectAccess(projectId, currentUser);
        ProjectSettlement settlement = settlementService.createSettlement(
                projectId,
                parseMonth(request.getYearMonth()),
                currentUser.getId()
        );
        return Result.success(settlement);
    }

    /**
     * 重新结算
     */
    @PostMapping("/{projectId}/reopen")
    @SaCheckPermission("settlement:edit")
    public Result<ProjectSettlement> reopenSettlement(
            @PathVariable Long projectId,
            @RequestBody SettlementActionRequest request) {
        CurrentUser currentUser = currentUserService.getCurrentUser();
        if (!currentUser.isAdmin() && !RoleConstants.BRANCH_ADMIN.equals(currentUser.getRole())) {
            return Result.error(403, "当前角色无权重新结算");
        }
        projectService.assertProjectAccess(projectId, currentUser);
        ProjectSettlement settlement = settlementService.reSettle(
                projectId,
                parseMonth(request.getYearMonth()),
                currentUser.getId()
        );
        return Result.success(settlement);
    }

    private SettlementListItemResponse toSettlementListItem(ProjectSettlement settlement) {
        Project project = projectService.getProjectRecord(settlement.getProjectId());
        SettlementListItemResponse item = new SettlementListItemResponse();
        item.setId(settlement.getId());
        item.setProjectId(settlement.getProjectId());
        item.setProjectName(project.getProjectName());
        item.setYearMonth(settlement.getSettlementMonth() == null ? null : YEAR_MONTH_FORMATTER.format(YearMonth.from(settlement.getSettlementMonth())));
        item.setStatus(settlement.getStatus());
        item.setAmount(settlement.getTotalAmount());
        item.setSettledAt(settlement.getUpdatedAt() == null ? settlement.getCreatedAt() : settlement.getUpdatedAt());
        return item;
    }

    private LocalDate parseMonth(String month) {
        return YearMonth.parse(month).atDay(1);
    }

    @Data
    static class SettlementActionRequest {
        private String yearMonth;
        private String reason;
    }
}
