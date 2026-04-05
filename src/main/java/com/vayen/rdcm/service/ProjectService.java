package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayen.rdcm.dto.MonthlyDataDetailResponse;
import com.vayen.rdcm.dto.OptionItemResponse;
import com.vayen.rdcm.dto.ProjectDetailResponse;
import com.vayen.rdcm.entity.Device;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.entity.ProjectEquipment;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.entity.ProjectOperationLog;
import com.vayen.rdcm.entity.ProjectSettlement;
import com.vayen.rdcm.mapper.DeviceMapper;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.mapper.ProjectMapper;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.mapper.ProjectOperationLogMapper;
import com.vayen.rdcm.mapper.ProjectSettlementMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 项目聚合服务。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ProjectMapper projectMapper;
    private final ProjectEmployeeService projectEmployeeService;
    private final ProjectEquipmentService projectEquipmentService;
    private final DeviceMapper deviceMapper;
    private final EmployeeMapper employeeMapper;
    private final ProjectOperationLogMapper projectOperationLogMapper;
    private final ProjectSettlementMapper projectSettlementMapper;
    private final ProjectMonthlyDataMapper projectMonthlyDataMapper;

    public Page<Project> getProjects(CurrentUser currentUser, Integer page, Integer size, String status, String name) {
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (StringUtils.hasText(name)) {
            wrapper.like("project_name", name.trim());
        }
        wrapper.orderByDesc("created_at");
        return projectMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Project getProjectById(Long projectId, CurrentUser currentUser) {
        Project project = getProjectRecord(projectId);
        assertProjectAccess(project, currentUser);
        return project;
    }

    public ProjectDetailResponse getProjectDetail(Long projectId, CurrentUser currentUser) {
        Project project = getProjectById(projectId, currentUser);
        List<ProjectEmployee> projectEmployees = projectEmployeeService.getByProjectId(projectId);
        List<ProjectEquipment> projectEquipments = projectEquipmentService.getByProjectId(projectId);
        Map<Long, Device> deviceMap = loadDevices(projectEquipments);
        List<ProjectOperationLog> logs = loadProjectLogs(projectId, currentUser);
        ProjectSettlement latestSettlement = loadLatestSettlement(projectId);

        ProjectDetailResponse response = new ProjectDetailResponse();
        response.setId(project.getId());
        response.setProjectName(project.getProjectName());
        response.setCode(project.getCode());
        response.setDescription(project.getDescription());
        response.setStartDate(project.getStartDate());
        response.setEndDate(project.getEndDate());
        response.setStatus(project.getStatus());
        response.setCompanyId(project.getCompanyId());
        response.setManagerName(project.getManagerName());
        response.setManagerPhone(project.getManagerPhone());
        response.setEmployeeCount(projectEmployees.size());
        response.setDeviceCount(projectEquipments.size());
        response.setSettlementAmount(latestSettlement == null ? 0.0 : latestSettlement.getTotalAmount());
        response.setEmployees(projectEmployees.stream().map(this::toProjectEmployeeItem).toList());
        response.setDevices(projectEquipments.stream().map(item -> toProjectDeviceItem(item, deviceMap.get(item.getDeviceId()))).toList());
        response.setLogs(logs.stream().map(this::toProjectLogItem).toList());
        return response;
    }

    public MonthlyDataDetailResponse getMonthlyDetail(Long projectId, LocalDate workMonth, CurrentUser currentUser) {
        getProjectById(projectId, currentUser);
        ProjectMonthlyData monthlyData = getProjectMonthlyDataRecord(projectId, workMonth);
        if (monthlyData == null) {
            throw new IllegalArgumentException("当前月份没有月度数据");
        }

        List<ProjectEmployee> projectEmployees = projectEmployeeService.getByProjectId(projectId);
        List<ProjectEquipment> projectEquipments = projectEquipmentService.getByProjectId(projectId);
        Map<Long, Device> deviceMap = loadDevices(projectEquipments);

        List<MonthlyDataDetailResponse.EmployeeItem> availableEmployees = projectEmployees.stream()
                .map(this::toMonthlyEmployeeItem)
                .toList();
        List<MonthlyDataDetailResponse.DeviceItem> availableDevices = projectEquipments.stream()
                .map(item -> toMonthlyDeviceItem(item, deviceMap.get(item.getDeviceId())))
                .toList();

        MonthlyDataDetailResponse response = new MonthlyDataDetailResponse();
        response.setProjectId(projectId);
        response.setYearMonth(YEAR_MONTH_FORMATTER.format(YearMonth.from(workMonth)));
        response.setStatus(monthlyData.getStatus());
        response.setGrandTotal(monthlyData.getGrandTotal());
        response.setSettledAt("settled".equals(monthlyData.getStatus()) && monthlyData.getUpdatedAt() != null
                ? monthlyData.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null);
        response.setAvailableEmployees(availableEmployees);
        response.setEmployees(resolveMonthlyEmployees(monthlyData, availableEmployees));
        response.setAvailableDevices(availableDevices);
        response.setDevices(resolveMonthlyDevices(monthlyData, availableDevices));
        response.setFees(JsonUtils.toFrontendFees(monthlyData.getCostData()));
        return response;
    }

    public List<OptionItemResponse> getEmployeeOptions(CurrentUser currentUser, String keyword) {
        QueryWrapper<com.vayen.rdcm.entity.Employee> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("employee_id", keyword).or().like("name", keyword).or().like("department", keyword));
        }
        wrapper.orderByAsc("name");
        return employeeMapper.selectList(wrapper).stream()
                .map(item -> new OptionItemResponse(item.getId(), item.getEmployeeId(), item.getName(), item.getDepartment()))
                .toList();
    }

    public List<OptionItemResponse> getDeviceOptions(CurrentUser currentUser, String keyword) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like("device_name", keyword).or().like("model", keyword));
        }
        wrapper.orderByAsc("device_name");
        return deviceMapper.selectList(wrapper).stream()
                .map(item -> new OptionItemResponse(item.getId(), String.valueOf(item.getId()), item.getDeviceName(), item.getModel()))
                .toList();
    }

    public Project getProjectRecord(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        return project;
    }

    public Project createProject(Project project, List<Long> employeeIds, List<Long> deviceIds, CurrentUser currentUser) {
        validateProjectInput(project);
        project.setId(null);
        if (!currentUser.isAdmin()) {
            project.setCompanyId(currentUser.getCompanyId());
        }
        if (!StringUtils.hasText(project.getStatus())) {
            project.setStatus("ongoing");
        }
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);
        projectEmployeeService.replaceProjectEmployees(project.getId(), project.getCompanyId(), employeeIds);
        projectEquipmentService.replaceProjectEquipments(project.getId(), deviceIds);
        return project;
    }

    public void updateProject(Long projectId, Project updateRequest, List<Long> employeeIds, List<Long> deviceIds, CurrentUser currentUser) {
        validateProjectInput(updateRequest);
        Project existing = getProjectById(projectId, currentUser);
        existing.setProjectName(updateRequest.getProjectName());
        existing.setCode(updateRequest.getCode());
        existing.setDescription(updateRequest.getDescription());
        existing.setManagerName(updateRequest.getManagerName());
        existing.setManagerPhone(updateRequest.getManagerPhone());
        existing.setStartDate(updateRequest.getStartDate());
        existing.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(existing);
        projectEmployeeService.replaceProjectEmployees(existing.getId(), existing.getCompanyId(), employeeIds);
        projectEquipmentService.replaceProjectEquipments(existing.getId(), deviceIds);
    }

    public void deleteProject(Long projectId, CurrentUser currentUser) {
        Project existing = getProjectById(projectId, currentUser);
        projectMapper.deleteById(existing.getId());
    }

    public void updateStatus(Long projectId, String status, CurrentUser currentUser) {
        Project existing = getProjectById(projectId, currentUser);
        existing.setStatus(status);
        if ("ended".equals(status) && existing.getEndDate() == null) {
            existing.setEndDate(LocalDate.now());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(existing);
    }

    public List<Long> getOngoingProjectIds() {
        QueryWrapper<Project> wrapper = new QueryWrapper<>();
        wrapper.select("id").eq("status", "ongoing");
        return projectMapper.selectObjs(wrapper).stream()
                .map(obj -> ((Number) obj).longValue())
                .collect(Collectors.toList());
    }

    public void assertProjectAccess(Long projectId, CurrentUser currentUser) {
        Project project = getProjectRecord(projectId);
        assertProjectAccess(project, currentUser);
    }

    private void assertProjectAccess(Project project, CurrentUser currentUser) {
        if (!currentUser.isAdmin() && !currentUser.getCompanyId().equals(project.getCompanyId())) {
            throw new IllegalArgumentException("禁止访问其他公司的项目");
        }
    }

    private Map<Long, Device> loadDevices(List<ProjectEquipment> projectEquipments) {
        List<Long> deviceIds = projectEquipments.stream()
                .map(ProjectEquipment::getDeviceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (deviceIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.in("id", deviceIds);
        return deviceMapper.selectList(wrapper).stream().collect(Collectors.toMap(Device::getId, item -> item));
    }

    private List<ProjectOperationLog> loadProjectLogs(Long projectId, CurrentUser currentUser) {
        QueryWrapper<ProjectOperationLog> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId);
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        wrapper.orderByDesc("created_at");
        return projectOperationLogMapper.selectList(wrapper);
    }

    private ProjectSettlement loadLatestSettlement(Long projectId) {
        QueryWrapper<ProjectSettlement> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId).orderByDesc("settlement_month").last("limit 1");
        return projectSettlementMapper.selectOne(wrapper);
    }

    private ProjectMonthlyData getProjectMonthlyDataRecord(Long projectId, LocalDate workMonth) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .eq("work_month", workMonth.atTime(0, 0, 0));
        return projectMonthlyDataMapper.selectOne(wrapper);
    }

    private ProjectDetailResponse.ProjectEmployeeItem toProjectEmployeeItem(ProjectEmployee employee) {
        ProjectDetailResponse.ProjectEmployeeItem item = new ProjectDetailResponse.ProjectEmployeeItem();
        item.setId(employee.getId());
        item.setEmployeeId(employee.getEmployeeId());
        item.setEmployeeName(employee.getEmployeeName());
        item.setEmployeeType(employee.getEmployeeType());
        item.setCoefficient(employee.getCoefficient());
        item.setDepartment(employee.getDepartment());
        item.setPhone(employee.getPhone());
        item.setEmail(employee.getEmail());
        return item;
    }

    private ProjectDetailResponse.ProjectDeviceItem toProjectDeviceItem(ProjectEquipment equipment, Device device) {
        ProjectDetailResponse.ProjectDeviceItem item = new ProjectDetailResponse.ProjectDeviceItem();
        item.setId(equipment.getId());
        item.setDeviceId(equipment.getDeviceId());
        if (device != null) {
            item.setDeviceName(device.getDeviceName());
            item.setModel(device.getModel());
            item.setStatus(device.getStatus());
            item.setDailyDepreciation(device.getDailyDepreciation());
            item.setMonthlyRental(device.getMonthlyRental());
        }
        return item;
    }

    private ProjectDetailResponse.ProjectLogItem toProjectLogItem(ProjectOperationLog log) {
        ProjectDetailResponse.ProjectLogItem item = new ProjectDetailResponse.ProjectLogItem();
        item.setId(log.getId());
        item.setAction(log.getAction());
        item.setTargetType(log.getTargetType());
        item.setTargetId(log.getTargetId());
        item.setRemark(log.getRemark());
        item.setOperatorId(log.getOperatorId());
        item.setCreatedAt(log.getCreatedAt());
        return item;
    }

    private MonthlyDataDetailResponse.EmployeeItem toMonthlyEmployeeItem(ProjectEmployee employee) {
        MonthlyDataDetailResponse.EmployeeItem item = new MonthlyDataDetailResponse.EmployeeItem();
        item.setEmployeeId(employee.getEmployeeId());
        item.setEmployeeNo(employee.getEmployeeId() == null ? null : String.valueOf(employee.getEmployeeId()));
        item.setName(employee.getEmployeeName());
        item.setDepartment(employee.getDepartment());
        item.setEmployeeType(employee.getEmployeeType());
        item.setCoefficient(employee.getCoefficient());
        item.setHourlyRate(null);
        return item;
    }

    private MonthlyDataDetailResponse.DeviceItem toMonthlyDeviceItem(ProjectEquipment equipment, Device device) {
        MonthlyDataDetailResponse.DeviceItem item = new MonthlyDataDetailResponse.DeviceItem();
        item.setDeviceId(equipment.getDeviceId());
        item.setDeviceNo(equipment.getDeviceId() == null ? null : String.valueOf(equipment.getDeviceId()));
        if (device != null) {
            item.setName(device.getDeviceName());
            item.setCategory(device.getModel());
            item.setDepreciationRate(device.getDailyDepreciation());
        }
        item.setIsUsed(Boolean.TRUE);
        return item;
    }

    private List<MonthlyDataDetailResponse.EmployeeItem> resolveMonthlyEmployees(
            ProjectMonthlyData monthlyData,
            List<MonthlyDataDetailResponse.EmployeeItem> fallbackEmployees) {
        if (!StringUtils.hasText(monthlyData.getEmployeeData())) {
            return fallbackEmployees;
        }
        try {
            return OBJECT_MAPPER.readValue(
                    monthlyData.getEmployeeData(),
                    new TypeReference<List<MonthlyDataDetailResponse.EmployeeItem>>() {}
            );
        } catch (Exception ex) {
            return fallbackEmployees;
        }
    }

    private List<MonthlyDataDetailResponse.DeviceItem> resolveMonthlyDevices(
            ProjectMonthlyData monthlyData,
            List<MonthlyDataDetailResponse.DeviceItem> fallbackDevices) {
        if (!StringUtils.hasText(monthlyData.getDeviceData())) {
            return fallbackDevices;
        }
        try {
            return OBJECT_MAPPER.readValue(
                    monthlyData.getDeviceData(),
                    new TypeReference<List<MonthlyDataDetailResponse.DeviceItem>>() {}
            );
        } catch (Exception ex) {
            return fallbackDevices;
        }
    }

    private void validateProjectInput(Project project) {
        if (!StringUtils.hasText(project.getProjectName())) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (!StringUtils.hasText(project.getCode())) {
            throw new IllegalArgumentException("项目编号不能为空");
        }
        if (project.getStartDate() == null) {
            throw new IllegalArgumentException("开始日期不能为空");
        }
    }
}
