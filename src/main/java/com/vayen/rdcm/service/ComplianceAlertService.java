package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.dto.ComplianceAlertResponse;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.ProjectMapper;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.util.JsonUtils;
import com.vayen.rdcm.util.MonthlyFeeCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 研发费用口径合规预警服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceAlertService {

    private static final BigDecimal DEDUCTION_OTHER_LIMIT = BigDecimal.valueOf(0.20);
    private static final BigDecimal HIGHTECH_OTHER_LIMIT = BigDecimal.valueOf(0.10);
    private static final String OTHER_CATEGORY_CODE = "other";
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ProjectMapper projectMapper;
    private final ProjectMonthlyDataMapper projectMonthlyDataMapper;

    public List<ComplianceAlertResponse> listAlerts(CurrentUser currentUser) {
        QueryWrapper<Project> projectWrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            projectWrapper.eq("company_id", currentUser.getCompanyId());
        }
        projectWrapper.orderByDesc("start_date").orderByDesc("id");
        List<Project> projects = projectMapper.selectList(projectWrapper);
        if (projects.isEmpty()) {
            return List.of();
        }

        List<Long> projectIds = projects.stream().map(Project::getId).filter(Objects::nonNull).toList();
        QueryWrapper<ProjectMonthlyData> monthlyWrapper = new QueryWrapper<>();
        monthlyWrapper.in("project_id", projectIds).orderByDesc("work_month").orderByDesc("id");
        List<ProjectMonthlyData> monthlyDataList = projectMonthlyDataMapper.selectList(monthlyWrapper);

        Map<Long, ProjectMonthlyData> latestMonthlyByProject = monthlyDataList.stream().collect(
                Collectors.toMap(ProjectMonthlyData::getProjectId, item -> item, (left, right) -> left, LinkedHashMap::new)
        );

        List<ComplianceAlertResponse> alerts = new ArrayList<>();
        for (Project project : projects) {
            ProjectMonthlyData monthlyData = latestMonthlyByProject.get(project.getId());
            if (monthlyData == null) {
                continue;
            }
            alerts.addAll(buildAlerts(project, monthlyData));
        }

        log.info("用户 {} 触发合规提醒校验，返回 {} 条提醒", currentUser.getUsername(), alerts.size());
        return alerts;
    }

    private List<ComplianceAlertResponse> buildAlerts(Project project, ProjectMonthlyData monthlyData) {
        JsonUtils.CostData costData = JsonUtils.parseCostData(monthlyData.getCostData());
        List<ComplianceAlertResponse> alerts = new ArrayList<>();

        maybeAppendAlert(alerts, project, monthlyData, costData, "deduction", "加计扣除", DEDUCTION_OTHER_LIMIT);
        maybeAppendAlert(alerts, project, monthlyData, costData, "hightech", "高新认定", HIGHTECH_OTHER_LIMIT);

        return alerts;
    }

    private void maybeAppendAlert(
            List<ComplianceAlertResponse> alerts,
            Project project,
            ProjectMonthlyData monthlyData,
            JsonUtils.CostData costData,
            String scopeCode,
            String scopeLabel,
            BigDecimal limit
    ) {
        BigDecimal total = calculateScopedTotal(costData, scopeCode);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal otherTotal = calculateScopedCategoryTotal(costData, OTHER_CATEGORY_CODE, scopeCode);
        BigDecimal ratio = otherTotal.divide(total, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(limit) <= 0) {
            return;
        }

        ComplianceAlertResponse item = new ComplianceAlertResponse();
        item.setProjectId(project.getId());
        item.setProjectName(project.getProjectName());
        item.setScopeCode(scopeCode);
        item.setScopeLabel(scopeLabel);
        item.setWorkMonth(Optional.ofNullable(monthlyData.getWorkMonth()).map(value -> YEAR_MONTH_FORMATTER.format(value)).orElse(null));
        String projectDisplayName = project.getProjectName().contains("项目")
                ? project.getProjectName()
                : project.getProjectName() + "项目";
        item.setMessage(projectDisplayName + scopeLabel + "口径核算不符合国家要求，请尽快修正");
        alerts.add(item);
    }

    private BigDecimal calculateScopedTotal(JsonUtils.CostData costData, String scopeCode) {
        BigDecimal total = BigDecimal.ZERO;
        for (String categoryCode : MonthlyFeeCatalog.categoryCodes()) {
            total = total.add(calculateScopedCategoryTotal(costData, categoryCode, scopeCode));
        }
        return total;
    }

    private BigDecimal calculateScopedCategoryTotal(JsonUtils.CostData costData, String categoryCode, String scopeCode) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : costData.getItems(categoryCode)) {
            if (!MonthlyFeeCatalog.isIncludedInScope(resolveItemLabel(categoryCode, item), scopeCode)) {
                continue;
            }
            total = total.add(BigDecimal.valueOf(readAmount(item)));
        }
        return total;
    }

    private String resolveItemLabel(String categoryCode, Map<String, Object> item) {
        Object itemLabel = item.get("itemLabel");
        if (itemLabel instanceof String value && !value.isBlank()) {
            return value;
        }
        Object label = item.get("label");
        if (label instanceof String value && !value.isBlank()) {
            return value;
        }
        Object itemCode = item.get("itemCode");
        if (itemCode instanceof String value && !value.isBlank()) {
            return MonthlyFeeCatalog.itemLabel(categoryCode, value);
        }
        return "";
    }

    private double readAmount(Map<String, Object> item) {
        Object amount = item.get("amount");
        if (amount instanceof Number value) {
            return value.doubleValue();
        }
        return 0D;
    }
}
