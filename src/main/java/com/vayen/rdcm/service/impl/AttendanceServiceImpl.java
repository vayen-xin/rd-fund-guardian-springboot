package com.vayen.rdcm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vayen.rdcm.dto.AttendanceDtos;
import com.vayen.rdcm.dto.PageResponse;
import com.vayen.rdcm.entity.AttendanceRecord;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.mapper.AttendanceRecordMapper;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.mapper.ProjectMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.service.AttendanceService;
import com.vayen.rdcm.service.ProjectEmployeeService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final String SOURCE_IMPORT = "system_import";
    private static final String SOURCE_MANUAL = "manual";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final ProjectMapper projectMapper;
    private final ProjectEmployeeService projectEmployeeService;

    /**
     * 查询打卡记录列表。
     */
    @Override
    public PageResponse<AttendanceDtos.AttendanceListItem> list(CurrentUser currentUser, Integer page, Integer size, String employeeId, String name, String projectCode, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq(AttendanceRecord::getCompanyId, currentUser.getCompanyId());
        }
        if (StringUtils.hasText(employeeId)) {
            wrapper.like(AttendanceRecord::getEmployeeNo, employeeId.trim());
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(AttendanceRecord::getEmployeeName, name.trim());
        }
        if (StringUtils.hasText(projectCode)) {
            wrapper.like(AttendanceRecord::getProjectCode, projectCode.trim());
        }
        if (startDate != null) {
            wrapper.ge(AttendanceRecord::getWorkDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(AttendanceRecord::getWorkDate, endDate);
        }
        wrapper.orderByDesc(AttendanceRecord::getWorkDate)
                .orderByAsc(AttendanceRecord::getProjectCode)
                .orderByAsc(AttendanceRecord::getEmployeeNo);

        Page<AttendanceRecord> result = attendanceRecordMapper.selectPage(new Page<>(page, size), wrapper);
        List<AttendanceDtos.AttendanceListItem> list = result.getRecords().stream().map(this::toListItem).toList();
        return new PageResponse<>(list, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 按工号或姓名匹配员工。
     */
    @Override
    public AttendanceDtos.AttendanceLookupResponse lookup(CurrentUser currentUser, String employeeId, String name) {
        AttendanceDtos.AttendanceLookupResponse response = new AttendanceDtos.AttendanceLookupResponse();
        Employee employee = findEmployee(currentUser, employeeId, name);
        if (employee == null) {
            response.setExactMatch(false);
            response.setHint("未找到匹配员工，请继续输入更完整的工号或姓名");
            return response;
        }
        response.setEmployeeId(employee.getEmployeeId());
        response.setName(employee.getName());
        response.setDepartment(employee.getDepartment());
        response.setExactMatch(exactEmployeeMatch(employee, employeeId, name));
        response.setHint(response.isExactMatch() ? "已自动匹配到员工信息" : "已为你匹配最接近的员工，请确认后再保存");
        return response;
    }

    /**
     * 预解析打卡导入模板。
     */
    @Override
    public AttendanceDtos.AttendanceImportPreviewResponse previewImport(CurrentUser currentUser, MultipartFile file, YearMonth month) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请先上传打卡文件");
        }

        List<AttendanceDtos.AttendanceImportRow> rows = new ArrayList<>();
        int failedCount = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isEmptyRow(row, formatter)) {
                    continue;
                }

                String employeeNo = readCell(row.getCell(1), formatter);
                String employeeName = readCell(row.getCell(2), formatter);
                String projectCode = readCell(row.getCell(3), formatter);
                if (!StringUtils.hasText(employeeNo) || !StringUtils.hasText(employeeName) || !StringUtils.hasText(projectCode)) {
                    failedCount++;
                    continue;
                }

                Project matchedProject = findProject(currentUser, projectCode);
                String projectName = matchedProject == null ? "" : matchedProject.getProjectName();

                for (int day = 1; day <= 31; day++) {
                    String text = readCell(row.getCell(3 + day), formatter);
                    if (!StringUtils.hasText(text) || "-".equals(text)) {
                        continue;
                    }
                    if (day > month.lengthOfMonth()) {
                        failedCount++;
                        continue;
                    }
                    BigDecimal duration = parseDecimal(text);
                    if (duration == null || duration.compareTo(BigDecimal.ZERO) < 0) {
                        failedCount++;
                        continue;
                    }

                    AttendanceDtos.AttendanceImportRow previewRow = new AttendanceDtos.AttendanceImportRow();
                    previewRow.setEmployeeId(employeeNo);
                    previewRow.setName(employeeName);
                    previewRow.setProjectCode(projectCode);
                    previewRow.setProjectName(projectName);
                    previewRow.setDate(month.atDay(day));
                    previewRow.setDuration(duration);
                    previewRow.setSource(SOURCE_IMPORT);
                    rows.add(previewRow);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取打卡文件失败", e);
        }

        rows.sort(Comparator.comparing(AttendanceDtos.AttendanceImportRow::getDate)
                .thenComparing(AttendanceDtos.AttendanceImportRow::getProjectCode)
                .thenComparing(AttendanceDtos.AttendanceImportRow::getEmployeeId));

        AttendanceDtos.AttendanceImportPreviewResponse response = new AttendanceDtos.AttendanceImportPreviewResponse();
        response.setRows(rows);
        response.setSuccessCount(rows.size());
        response.setFailedCount(failedCount);
        return response;
    }

    /**
     * 确认导入预解析后的打卡记录。
     */
    @Override
    public void confirmImport(CurrentUser currentUser, List<AttendanceDtos.AttendanceImportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("没有可导入的打卡记录");
        }
        for (AttendanceDtos.AttendanceImportRow row : rows) {
            upsert(currentUser, row.getEmployeeId(), row.getName(), row.getProjectCode(), row.getDate(), row.getDuration(), SOURCE_IMPORT);
        }
    }

    /**
     * 手动新增打卡记录。
     */
    @Override
    public void save(CurrentUser currentUser, AttendanceDtos.AttendanceSaveRequest request) {
        validateSaveRequest(request);
        upsert(currentUser, request.getEmployeeId(), request.getName(), request.getProjectCode(), request.getDate(), request.getDuration(), SOURCE_MANUAL);
    }

    /**
     * 修改打卡记录。
     */
    @Override
    public void update(CurrentUser currentUser, Long id, AttendanceDtos.AttendanceSaveRequest request) {
        validateSaveRequest(request);
        AttendanceRecord existing = getById(currentUser, id);
        Employee employee = findEmployee(currentUser, request.getEmployeeId(), request.getName());
        Project matchedProject = findProject(currentUser, request.getProjectCode());

        existing.setCompanyId(resolveCompanyId(currentUser, employee));
        existing.setEmployeeId(employee != null ? employee.getId() : null);
        existing.setEmployeeNo(request.getEmployeeId().trim());
        existing.setEmployeeName(request.getName().trim());
        existing.setProjectCode(request.getProjectCode().trim());
        existing.setProjectName(matchedProject == null ? null : matchedProject.getProjectName());
        existing.setWorkDate(request.getDate());
        existing.setDurationHours(request.getDuration());
        existing.setSource(SOURCE_MANUAL);
        existing.setUpdatedAt(LocalDateTime.now());
        attendanceRecordMapper.updateById(existing);
    }

    /**
     * 删除打卡记录。
     */
    @Override
    public void delete(CurrentUser currentUser, Long id) {
        AttendanceRecord existing = getById(currentUser, id);
        attendanceRecordMapper.deleteById(existing.getId());
    }

    /**
     * 下载导入模板。
     */
    @Override
    public Resource buildTemplate(CurrentUser currentUser, Long projectId, YearMonth month) {
        if (projectId == null) {
            Resource resource = new ClassPathResource("static/template.xlsx");
            if (!resource.exists()) {
                throw new IllegalArgumentException("未找到打卡导入模板");
            }
            return resource;
        }
        return buildProjectTemplate(currentUser, projectId, month == null ? YearMonth.now() : month);
    }

    private void validateSaveRequest(AttendanceDtos.AttendanceSaveRequest request) {
        if (!StringUtils.hasText(request.getEmployeeId()) || !StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("工号和姓名不能为空");
        }
        if (!StringUtils.hasText(request.getProjectCode())) {
            throw new IllegalArgumentException("项目号不能为空");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        if (request.getDuration() == null) {
            throw new IllegalArgumentException("打卡时长不能为空");
        }
    }

    /**
     * 按员工、项目和日期覆盖写入打卡记录。
     */
    private void upsert(CurrentUser currentUser, String employeeNo, String employeeName, String projectCode, LocalDate date, BigDecimal duration, String source) {
        Employee employee = findEmployee(currentUser, employeeNo, employeeName);
        Project matchedProject = findProject(currentUser, projectCode);
        Long companyId = resolveCompanyId(currentUser, employee);

        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getCompanyId, companyId)
                .eq(AttendanceRecord::getEmployeeNo, employeeNo.trim())
                .eq(AttendanceRecord::getProjectCode, projectCode.trim())
                .eq(AttendanceRecord::getWorkDate, date)
                .last("limit 1");
        AttendanceRecord existing = attendanceRecordMapper.selectOne(wrapper);

        if (existing == null) {
            existing = new AttendanceRecord();
            existing.setCompanyId(companyId);
            existing.setCreatedBy(currentUser.getId());
            existing.setCreatedAt(LocalDateTime.now());
        }

        existing.setEmployeeId(employee == null ? null : employee.getId());
        existing.setEmployeeNo(employeeNo.trim());
        existing.setEmployeeName(employeeName.trim());
        existing.setProjectCode(projectCode.trim());
        existing.setProjectName(matchedProject == null ? null : matchedProject.getProjectName());
        existing.setWorkDate(date);
        existing.setDurationHours(duration);
        existing.setSource(source);
        existing.setUpdatedAt(LocalDateTime.now());

        if (existing.getId() == null) {
            attendanceRecordMapper.insert(existing);
        } else {
            attendanceRecordMapper.updateById(existing);
        }
    }

    private AttendanceRecord getById(CurrentUser currentUser, Long id) {
        AttendanceRecord record = attendanceRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("打卡记录不存在");
        }
        if (!currentUser.isAdmin() && !record.getCompanyId().equals(currentUser.getCompanyId())) {
            throw new IllegalArgumentException("禁止访问其他公司的打卡数据");
        }
        return record;
    }

    /**
     * 按工号或姓名模糊匹配员工。
     */
    private Employee findEmployee(CurrentUser currentUser, String employeeId, String name) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq(Employee::getCompanyId, currentUser.getCompanyId());
        }
        if (StringUtils.hasText(employeeId) && StringUtils.hasText(name)) {
            wrapper.and(q -> q.eq(Employee::getEmployeeId, employeeId.trim()).or().eq(Employee::getName, name.trim()));
        } else if (StringUtils.hasText(employeeId)) {
            String keyword = employeeId.trim();
            wrapper.and(q -> q.eq(Employee::getEmployeeId, keyword).or().likeRight(Employee::getEmployeeId, keyword));
        } else if (StringUtils.hasText(name)) {
            String keyword = name.trim();
            wrapper.and(q -> q.eq(Employee::getName, keyword).or().likeRight(Employee::getName, keyword));
        } else {
            return null;
        }
        wrapper.orderByAsc(Employee::getEmployeeId).last("limit 5");
        List<Employee> employees = employeeMapper.selectList(wrapper);
        if (employees.isEmpty()) {
            return null;
        }
        employees.sort(Comparator.comparing(Employee::getEmployeeId, Comparator.nullsLast(String::compareTo)));
        return employees.get(0);
    }

    private boolean exactEmployeeMatch(Employee employee, String employeeId, String name) {
        boolean employeeIdMatched = StringUtils.hasText(employeeId) && employeeId.trim().equals(employee.getEmployeeId());
        boolean nameMatched = StringUtils.hasText(name) && name.trim().equals(employee.getName());
        return employeeIdMatched || nameMatched;
    }

    private Project findProject(CurrentUser currentUser, String projectCode) {
        if (!StringUtils.hasText(projectCode)) {
            return null;
        }
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        if (!currentUser.isAdmin()) {
            wrapper.eq(Project::getCompanyId, currentUser.getCompanyId());
        }
        wrapper.eq(Project::getCode, projectCode.trim()).last("limit 1");
        return projectMapper.selectOne(wrapper);
    }

    private Long resolveCompanyId(CurrentUser currentUser, Employee employee) {
        if (employee != null && employee.getCompanyId() != null) {
            return employee.getCompanyId();
        }
        if (currentUser.getCompanyId() != null) {
            return currentUser.getCompanyId();
        }
        throw new IllegalArgumentException("当前账号未绑定公司，无法导入打卡记录");
    }

    private AttendanceDtos.AttendanceListItem toListItem(AttendanceRecord record) {
        AttendanceDtos.AttendanceListItem item = new AttendanceDtos.AttendanceListItem();
        item.setId(record.getId());
        item.setEmployeeId(record.getEmployeeNo());
        item.setName(record.getEmployeeName());
        item.setProjectCode(record.getProjectCode());
        item.setProjectName(record.getProjectName());
        item.setDate(record.getWorkDate());
        item.setDuration(record.getDurationHours());
        item.setSource(SOURCE_IMPORT.equals(record.getSource()) ? "系统导入" : "手动录入");
        return item;
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        for (int i = 1; i <= 34; i++) {
            if (StringUtils.hasText(readCell(row.getCell(i), formatter))) {
                return false;
            }
        }
        return true;
    }

    private String readCell(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private BigDecimal parseDecimal(String text) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static YearMonth parseMonth(String month) {
        if (!StringUtils.hasText(month)) {
            throw new IllegalArgumentException("请先选择导入月份");
        }
        try {
            return YearMonth.parse(month.trim(), MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("导入月份格式不正确，应为 yyyy-MM");
        }
    }

    private Resource buildProjectTemplate(CurrentUser currentUser, Long projectId, YearMonth month) {
        Project project = getProjectWithAccess(currentUser, projectId);
        List<ProjectEmployee> projectEmployees = projectEmployeeService.getByProjectId(projectId);
        Map<Long, Employee> employeeMap = loadEmployeeMap(projectEmployees);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("attendance-template");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("序号");
            header.createCell(1).setCellValue("工号");
            header.createCell(2).setCellValue("姓名");
            header.createCell(3).setCellValue("项目号");
            for (int day = 1; day <= 31; day++) {
                header.createCell(3 + day).setCellValue(day);
            }

            for (int index = 0; index < projectEmployees.size(); index++) {
                ProjectEmployee relation = projectEmployees.get(index);
                Employee employee = employeeMap.get(relation.getEmployeeId());
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue(index + 1);
                row.createCell(1).setCellValue(employee == null ? "" : employee.getEmployeeId());
                row.createCell(2).setCellValue(relation.getEmployeeName());
                row.createCell(3).setCellValue(project.getCode());
                for (int day = 1; day <= 31; day++) {
                    Cell cell = row.createCell(3 + day);
                    cell.setCellValue(day <= month.lengthOfMonth() ? "-" : "");
                }
            }

            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 14 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 14 * 256);
            for (int col = 4; col <= 34; col++) {
                sheet.setColumnWidth(col, 5 * 256);
            }
            sheet.createFreezePane(4, 1);

            workbook.write(outputStream);
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("生成动态打卡模板失败", e);
        }
    }

    private Project getProjectWithAccess(CurrentUser currentUser, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("项目不存在");
        }
        if (!currentUser.isAdmin() && !project.getCompanyId().equals(currentUser.getCompanyId())) {
            throw new IllegalArgumentException("禁止访问其他公司的项目");
        }
        return project;
    }

    private Map<Long, Employee> loadEmployeeMap(List<ProjectEmployee> projectEmployees) {
        Set<Long> employeeIds = projectEmployees.stream()
                .map(ProjectEmployee::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.in("id", employeeIds);
        return employeeMapper.selectList(wrapper).stream().collect(Collectors.toMap(Employee::getId, item -> item));
    }
}
