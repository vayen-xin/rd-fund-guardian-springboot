package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.security.RoleConstants;
import com.vayen.rdcm.service.ProjectMonthlyDataService;
import com.vayen.rdcm.service.ProjectService;
import com.vayen.rdcm.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目月度费用服务实现。
 */
@Service
@RequiredArgsConstructor
public class ProjectMonthlyDataServiceImpl extends ServiceImpl<ProjectMonthlyDataMapper, ProjectMonthlyData>
        implements ProjectMonthlyDataService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProjectService projectService;

    private LocalDateTime toStartOfMonth(LocalDate month) {
        return month.atTime(0, 0, 0);
    }

    @Override
    public ProjectMonthlyData getProjectMonthlyData(Long projectId, LocalDate workMonth) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("work_month", toStartOfMonth(workMonth));
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMonthlyData(Long projectId, LocalDate workMonth, String costData,
                                String employeeData, String deviceData, Double grandTotal, CurrentUser currentUser) {
        ProjectMonthlyData monthlyData = getProjectMonthlyData(projectId, workMonth);
        if (monthlyData == null) {
            Project project = projectService.getProjectRecord(projectId);
            monthlyData = new ProjectMonthlyData();
            monthlyData.setCompanyId(project.getCompanyId());
            monthlyData.setProjectId(projectId);
            monthlyData.setWorkMonth(toStartOfMonth(workMonth));
            monthlyData.setCreatedBy(currentUser.getId());
            monthlyData.setCreatedAt(LocalDateTime.now());
        } else if ("settled".equals(monthlyData.getStatus()) && !canEditSettledMonthlyData(currentUser)) {
            throw new IllegalArgumentException("已结算的月度数据仅允许管理员修改");
        }

        double safeGrandTotal = grandTotal == null ? 0.0 : grandTotal;
        double calculatedTotal = JsonUtils.calculateGrandTotal(costData);
        if (Math.abs(calculatedTotal - safeGrandTotal) > 0.01) {
            throw new IllegalArgumentException(
                    String.format("JSON 计算总和(%.2f)与传入总金额(%.2f)不一致", calculatedTotal, safeGrandTotal)
            );
        }

        monthlyData.setCostData(costData);
        monthlyData.setEmployeeData(normalizeJsonArray(employeeData, "月度员工数据格式不正确"));
        monthlyData.setDeviceData(normalizeJsonArray(deviceData, "月度设备数据格式不正确"));
        monthlyData.setGrandTotal(safeGrandTotal);
        monthlyData.setUpdatedAt(LocalDateTime.now());

        JsonUtils.CostData parsed = JsonUtils.parseCostData(costData);
        monthlyData.setLaborTotal(parsed.getCategoryTotal("labor"));
        monthlyData.setDirectMaterialTotal(parsed.getCategoryTotal("direct"));
        monthlyData.setDirectFuelTotal(0.0);
        monthlyData.setDirectRentalTotal(0.0);
        monthlyData.setDepreciationTotal(parsed.getCategoryTotal("deprec") + parsed.getCategoryTotal("long_deferred"));
        monthlyData.setAmortizationTotal(parsed.getCategoryTotal("intangible"));
        monthlyData.setDesignTotal(parsed.getCategoryTotal("design"));
        monthlyData.setCommissioningTotal(parsed.getCategoryTotal("equip"));
        monthlyData.setOutsourcedTotal(parsed.getCategoryTotal("outsource"));
        monthlyData.setOtherTotal(parsed.getCategoryTotal("other"));
        monthlyData.setStatus("draft");
        monthlyData.setVersion(monthlyData.getVersion() == null ? 1 : monthlyData.getVersion() + 1);

        this.saveOrUpdate(monthlyData);
    }

    private boolean canEditSettledMonthlyData(CurrentUser currentUser) {
        String role = RoleConstants.normalize(currentUser.getRole());
        return RoleConstants.ADMIN.equals(role) || RoleConstants.BRANCH_ADMIN.equals(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitMonthlyData(Long projectId, LocalDate workMonth, Long operatorId) {
        ProjectMonthlyData monthlyData = getProjectMonthlyData(projectId, workMonth);
        if (monthlyData == null) {
            throw new IllegalArgumentException("当前月份没有草稿数据，无法提交");
        }
        if ("settled".equals(monthlyData.getStatus())) {
            throw new IllegalArgumentException("已结算的数据不能重复提交");
        }
        monthlyData.setStatus("finalized");
        monthlyData.setUpdatedAt(LocalDateTime.now());
        this.updateById(monthlyData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inheritFromLastMonth(Long projectId, LocalDate fromMonth, LocalDate toMonth, Long operatorId) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("work_month", toStartOfMonth(fromMonth));
        ProjectMonthlyData lastMonthData = this.getOne(wrapper);

        if (lastMonthData == null) {
            Project project = projectService.getProjectRecord(projectId);
            ProjectMonthlyData newData = new ProjectMonthlyData();
            newData.setCompanyId(project.getCompanyId());
            newData.setProjectId(projectId);
            newData.setWorkMonth(toStartOfMonth(toMonth));
            newData.setCostData("{}");
            newData.setEmployeeData("[]");
            newData.setDeviceData("[]");
            newData.setGrandTotal(0.0);
            newData.setLaborTotal(0.0);
            newData.setDirectMaterialTotal(0.0);
            newData.setDirectFuelTotal(0.0);
            newData.setDirectRentalTotal(0.0);
            newData.setDepreciationTotal(0.0);
            newData.setAmortizationTotal(0.0);
            newData.setDesignTotal(0.0);
            newData.setCommissioningTotal(0.0);
            newData.setOutsourcedTotal(0.0);
            newData.setOtherTotal(0.0);
            newData.setStatus("draft");
            newData.setCreatedBy(operatorId);
            newData.setCreatedAt(LocalDateTime.now());
            newData.setUpdatedAt(LocalDateTime.now());
            this.save(newData);
            return;
        }

        ProjectMonthlyData newData = new ProjectMonthlyData();
        newData.setCompanyId(lastMonthData.getCompanyId());
        newData.setProjectId(projectId);
        newData.setWorkMonth(toStartOfMonth(toMonth));
        newData.setCostData(JsonUtils.zeroOutAmounts(lastMonthData.getCostData()));
        newData.setEmployeeData(lastMonthData.getEmployeeData());
        newData.setDeviceData(lastMonthData.getDeviceData());
        newData.setGrandTotal(0.0);
        newData.setLaborTotal(0.0);
        newData.setDirectMaterialTotal(0.0);
        newData.setDirectFuelTotal(0.0);
        newData.setDirectRentalTotal(0.0);
        newData.setDepreciationTotal(0.0);
        newData.setAmortizationTotal(0.0);
        newData.setDesignTotal(0.0);
        newData.setCommissioningTotal(0.0);
        newData.setOutsourcedTotal(0.0);
        newData.setOtherTotal(0.0);
        newData.setStatus("draft");
        newData.setCreatedBy(operatorId);
        newData.setCreatedAt(LocalDateTime.now());
        newData.setUpdatedAt(LocalDateTime.now());
        this.save(newData);
    }

    @Override
    public List<ProjectMonthlyData> getProjectMonthlyList(Long projectId) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .orderByDesc("work_month");
        return this.list(wrapper);
    }

    private String normalizeJsonArray(String json, String errorMessage) {
        if (json == null || json.isBlank()) {
            return "[]";
        }
        try {
            List<?> parsed = OBJECT_MAPPER.readValue(json, List.class);
            return OBJECT_MAPPER.writeValueAsString(parsed);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(errorMessage, e);
        }
    }
}
