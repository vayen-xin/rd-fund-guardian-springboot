package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayen.rdcm.config.AuditTemplateProperties;
import com.vayen.rdcm.entity.AttendanceRecord;
import com.vayen.rdcm.entity.Company;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.AttendanceRecordMapper;
import com.vayen.rdcm.mapper.CompanyMapper;
import com.vayen.rdcm.mapper.ProjectMapper;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.security.CurrentUser;
import com.vayen.rdcm.util.ExcelTemplateUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyWageExportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CompanyMapper companyMapper;
    private final ProjectMapper projectMapper;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ProjectMonthlyDataMapper projectMonthlyDataMapper;
    private final AuditTemplateProperties auditTemplateProperties;

    /**
     * 导出公司级研发工资明细表
     */
    public byte[] exportWorkbook(Long companyId, YearMonth startMonth, YearMonth endMonth, CurrentUser currentUser) {
        if (companyId == null) {
            throw new IllegalArgumentException("公司不能为空");
        }
        if (startMonth == null || endMonth == null || startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("导出时间范围不合法");
        }
        if (!currentUser.isAdmin() && !Objects.equals(companyId, currentUser.getCompanyId())) {
            throw new IllegalArgumentException("无权导出其他公司的工资明细");
        }

        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("公司不存在");
        }

        ExportContext context = buildContext(company, startMonth, endMonth);
        log.info("用户 {} 导出公司工资明细表，companyId={}，period={}~{}",
                currentUser.getUsername(), companyId, startMonth, endMonth);

        Workbook workbook = ExcelTemplateUtils.loadTemplate(auditTemplateProperties.getCompanyWagePath());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            pruneMonthSheets(workbook, context.getMonths());
            removeSheetByNameContains(workbook, "差异");
            writeInstructionSheet(workbook, context);
            writeRosterSheet(workbook, context);
            writeSummarySheet(workbook, context);
            for (YearMonth month : context.getMonths()) {
                Sheet monthlySheet = resolveMonthlySheet(workbook, month, false);
                Sheet attendanceSheet = resolveMonthlySheet(workbook, month, true);
                writeMonthlySheet(monthlySheet, context, month);
                writeAttendanceSheet(attendanceSheet, context, month);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("导出公司工资明细表失败", ex);
        }
    }

    private ExportContext buildContext(Company company, YearMonth startMonth, YearMonth endMonth) {
        LocalDate startDate = startMonth.atDay(1);
        LocalDate endDate = endMonth.atEndOfMonth();

        QueryWrapper<Project> projectWrapper = new QueryWrapper<>();
        projectWrapper.eq("company_id", company.getId())
                .le("start_date", endDate)
                .and(wrapper -> wrapper.isNull("end_date").or().ge("end_date", startDate))
                .orderByAsc("code");
        List<Project> projects = projectMapper.selectList(projectWrapper);
        Map<String, String> projectNameByCode = projects.stream()
                .filter(item -> StringUtils.hasText(item.getCode()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getCode(), item.getProjectName()), LinkedHashMap::putAll);
        Map<Long, String> projectCodeById = projects.stream()
                .filter(item -> StringUtils.hasText(item.getCode()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item.getCode()), LinkedHashMap::putAll);

        QueryWrapper<AttendanceRecord> attendanceWrapper = new QueryWrapper<>();
        attendanceWrapper.eq("company_id", company.getId())
                .ge("work_date", startDate)
                .le("work_date", endDate)
                .orderByAsc("work_date")
                .orderByAsc("employee_no");
        List<AttendanceRecord> attendanceRecords = attendanceRecordMapper.selectList(attendanceWrapper);

        QueryWrapper<ProjectMonthlyData> monthlyWrapper = new QueryWrapper<>();
        monthlyWrapper.eq("company_id", company.getId())
                .ge("work_month", startDate.atStartOfDay())
                .le("work_month", endDate.atTime(23, 59, 59))
                .orderByAsc("work_month");
        List<ProjectMonthlyData> monthlyDataList = projectMonthlyDataMapper.selectList(monthlyWrapper);

        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }

        Map<String, EmployeeAnnualRow> employees = buildEmployeeRows(attendanceRecords, monthlyDataList, projectNameByCode, months);
        applyAttendanceAllocation(employees, attendanceRecords, monthlyDataList, projectCodeById, months);
        return ExportContext.builder()
                .company(company)
                .months(months)
                .projectNameByCode(projectNameByCode)
                .employees(employees)
                .attendanceRecords(attendanceRecords)
                .monthlyDataList(monthlyDataList)
                .build();
    }

    private Map<String, EmployeeAnnualRow> buildEmployeeRows(List<AttendanceRecord> attendanceRecords,
                                                             List<ProjectMonthlyData> monthlyDataList,
                                                             Map<String, String> projectNameByCode,
                                                             List<YearMonth> months) {
        Map<String, EmployeeAnnualRow> rows = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRecords) {
            String key = employeeKey(record.getEmployeeNo(), record.getEmployeeName(), record.getProjectCode());
            EmployeeAnnualRow row = rows.computeIfAbsent(key, ignore -> EmployeeAnnualRow.builder()
                    .employeeNo(record.getEmployeeNo())
                    .employeeName(record.getEmployeeName())
                    .projectCode(record.getProjectCode())
                    .projectName(projectNameByCode.get(record.getProjectCode()))
                    .personnelAttribute("研发人员")
                    .projectManager(false)
                    .monthlyHours(new TreeMap<>())
                    .monthlyWages(new TreeMap<>())
                    .build());
            row.getMonthlyHours().merge(YearMonth.from(record.getWorkDate()), safe(record.getDurationHours()), BigDecimal::add);
        }
        for (ProjectMonthlyData monthlyData : monthlyDataList) {
            YearMonth month = YearMonth.from(monthlyData.getWorkMonth());
            for (WageRow wageRow : parseLaborRows(monthlyData.getCostData())) {
                String key = employeeKey(wageRow.getEmployeeNo(), wageRow.getEmployeeName(), wageRow.getProjectCode());
                EmployeeAnnualRow row = rows.computeIfAbsent(key, ignore -> EmployeeAnnualRow.builder()
                        .employeeNo(wageRow.getEmployeeNo())
                        .employeeName(wageRow.getEmployeeName())
                        .projectCode(wageRow.getProjectCode())
                        .projectName(projectNameByCode.get(wageRow.getProjectCode()))
                        .personnelAttribute(displayPersonnelAttribute(wageRow.getPersonnelAttribute()))
                        .projectManager(Boolean.TRUE.equals(wageRow.getProjectManager()))
                        .monthlyHours(new TreeMap<>())
                        .monthlyWages(new TreeMap<>())
                        .build());
                row.setPersonnelAttribute(displayPersonnelAttribute(wageRow.getPersonnelAttribute()));
                row.setProjectManager(Boolean.TRUE.equals(wageRow.getProjectManager()));
                row.getMonthlyHours().putIfAbsent(month, safe(wageRow.getRdHours()));
                row.getMonthlyWages().put(month, wageRow);
            }
        }
        months.forEach(month -> rows.values().forEach(item -> {
            item.getMonthlyHours().putIfAbsent(month, BigDecimal.ZERO);
            item.getMonthlyWages().putIfAbsent(month,
                    WageRow.empty(item.getEmployeeNo(), item.getEmployeeName(), item.getProjectCode(), item.getPersonnelAttribute(), item.isProjectManager()));
        }));
        return rows;
    }

    private void applyAttendanceAllocation(Map<String, EmployeeAnnualRow> employees,
                                           List<AttendanceRecord> attendanceRecords,
                                           List<ProjectMonthlyData> monthlyDataList,
                                           Map<Long, String> projectCodeById,
                                           List<YearMonth> months) {
        Map<String, BigDecimal> totalHoursByProjectMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> employeeHoursByProjectMonth = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRecords) {
            String projectCode = record.getProjectCode();
            if (!StringUtils.hasText(projectCode)) {
                continue;
            }
            YearMonth month = YearMonth.from(record.getWorkDate());
            String projectMonthKey = projectCode + "::" + month;
            BigDecimal hours = safe(record.getDurationHours());
            totalHoursByProjectMonth.merge(projectMonthKey, hours, BigDecimal::add);
            String employeeKey = employeeKey(record.getEmployeeNo(), record.getEmployeeName(), projectCode);
            String employeeMonthKey = employeeKey + "::" + month;
            employeeHoursByProjectMonth.merge(employeeMonthKey, hours, BigDecimal::add);
        }

        Map<String, BigDecimal> laborTotalByProjectMonth = new LinkedHashMap<>();
        for (ProjectMonthlyData monthlyData : monthlyDataList) {
            String projectCode = projectCodeById.get(monthlyData.getProjectId());
            if (!StringUtils.hasText(projectCode)) {
                continue;
            }
            YearMonth month = YearMonth.from(monthlyData.getWorkMonth());
            BigDecimal laborTotal = BigDecimal.valueOf(monthlyData.getLaborTotal() == null ? 0D : monthlyData.getLaborTotal());
            laborTotalByProjectMonth.put(projectCode + "::" + month, laborTotal);
        }

        for (EmployeeAnnualRow row : employees.values()) {
            for (YearMonth month : months) {
                WageRow wage = row.getMonthlyWages().get(month);
                if (wage == null) {
                    continue;
                }
                if (wage.getSalary() != null && wage.getSalary().compareTo(BigDecimal.ZERO) > 0) {
                    continue;
                }
                String projectMonthKey = row.getProjectCode() + "::" + month;
                BigDecimal laborTotal = laborTotalByProjectMonth.get(projectMonthKey);
                if (laborTotal == null || laborTotal.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal totalHours = totalHoursByProjectMonth.get(projectMonthKey);
                if (totalHours == null || totalHours.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                String employeeMonthKey = employeeKey(row.getEmployeeNo(), row.getEmployeeName(), row.getProjectCode()) + "::" + month;
                BigDecimal employeeHours = employeeHoursByProjectMonth.get(employeeMonthKey);
                if (employeeHours == null || employeeHours.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal allocated = laborTotal.multiply(employeeHours)
                        .divide(totalHours, 2, RoundingMode.HALF_UP);
                wage.setSalary(allocated);
            }
        }
    }

    private List<WageRow> parseLaborRows(String costData) {
        List<WageRow> rows = new ArrayList<>();
        if (!StringUtils.hasText(costData)) {
            return rows;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(costData);
            JsonNode laborNode = root.get("labor");
            if (laborNode == null || laborNode.isNull()) {
                return rows;
            }
            if (laborNode.isArray()) {
                laborNode.forEach(item -> rows.add(parseArrayLaborRow(item)));
            } else {
                appendObjectRows(rows, laborNode.get("systemItems"));
                appendObjectRows(rows, laborNode.get("manualItems"));
            }
            return rows;
        } catch (Exception ex) {
            log.warn("解析工资导出 labor 数据失败，已跳过该月记录");
            return rows;
        }
    }

    private WageRow parseArrayLaborRow(JsonNode item) {
        return WageRow.builder()
                .employeeNo(item.path("employee_no").asText(""))
                .employeeName(item.path("employee_name").asText(""))
                .projectCode(item.path("project_code").asText(""))
                .personnelAttribute(item.path("personnel_attribute").asText(""))
                .projectManager(item.path("is_project_manager").asBoolean(false))
                .totalHours(readDecimal(item.path("total_hours")))
                .rdHours(readDecimal(item.path("rd_hours")))
                .salary(readNestedAmount(item, "salary"))
                .pension(readNestedAmount(item, "pension"))
                .unemployment(readNestedAmount(item, "unemployment"))
                .medical(readNestedAmount(item, "medical"))
                .maternity(readNestedAmount(item, "maternity"))
                .injury(readNestedAmount(item, "injury"))
                .housingFund(readNestedAmount(item, "housing_fund"))
                .externalLabor(readNestedAmount(item, "external_labor"))
                .build();
    }

    private void appendObjectRows(List<WageRow> rows, JsonNode itemsNode) {
        if (itemsNode == null || !itemsNode.isArray()) {
            return;
        }
        itemsNode.forEach(item -> rows.add(WageRow.builder()
                .employeeNo(item.path("employeeNo").asText(""))
                .employeeName(item.path("name").asText(""))
                .projectCode(item.path("projectCode").asText(""))
                .personnelAttribute("研发人员")
                .projectManager(false)
                .totalHours(readDecimal(item.path("totalHours")))
                .rdHours(readDecimal(item.path("rdHours")))
                .salary(readDecimal(item.path("amount")))
                .build()));
    }

    private void writeInstructionSheet(Workbook workbook, ExportContext context) {
        Sheet sheet = ExcelTemplateUtils.findSheetByNameContains(workbook, "说明");
        if (sheet == null) {
            return;
        }
        int rowIndex = ExcelTemplateUtils.findRowIndexByCellContains(sheet, "公司名称");
        if (rowIndex >= 0) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                setCellValue(row, row.getFirstCellNum() + 1, context.getCompany().getName(), null);
            }
        }
        int rangeRow = ExcelTemplateUtils.findRowIndexByCellContains(sheet, "导出范围");
        if (rangeRow >= 0) {
            Row row = sheet.getRow(rangeRow);
            if (row != null) {
                String range = context.getMonths().get(0) + " 至 " + context.getMonths().get(context.getMonths().size() - 1);
                setCellValue(row, row.getFirstCellNum() + 1, range, null);
            }
        }
    }

    private void writeRosterSheet(Workbook workbook, ExportContext context) {
        Sheet sheet = ExcelTemplateUtils.findSheetByNameContains(workbook, "人员清单");
        if (sheet == null) {
            sheet = ExcelTemplateUtils.findSheetByNameContains(workbook, "人员名单");
        }
        if (sheet == null) {
            return;
        }
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex < 0) {
            return;
        }
        Row headerRow = sheet.getRow(headerRowIndex);
        int dataStartRow = headerRowIndex + 1;
        int colSeq = findColumnIndexContains(headerRow, "序号", 0);
        int colEmpNo = findColumnIndexContains(headerRow, "工号", 1);
        int colName = findColumnIndexContains(headerRow, "姓名", 2);
        int colProjectCode = findColumnIndexContains(headerRow, "项目号", 3);
        int colProjectName = findColumnIndexContains(headerRow, "项目名", 4);
        int colAttr = findColumnIndexContains(headerRow, "人员", 5);
        int colManager = findColumnIndexContains(headerRow, "负责人", 6);
        int colMonths = findColumnIndexContains(headerRow, "月份", 7);

        clearDataRows(sheet, dataStartRow, colSeq);
        Row styleRow = sheet.getRow(dataStartRow);

        int rowIndex = dataStartRow;
        int seq = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            Row row = ensureRow(sheet, rowIndex++, styleRow);
            setCellValue(row, colSeq, seq++, cellFromRow(styleRow, colSeq));
            setCellValue(row, colEmpNo, rowData.getEmployeeNo(), cellFromRow(styleRow, colEmpNo));
            setCellValue(row, colName, rowData.getEmployeeName(), cellFromRow(styleRow, colName));
            setCellValue(row, colProjectCode, rowData.getProjectCode(), cellFromRow(styleRow, colProjectCode));
            setCellValue(row, colProjectName, rowData.getProjectName(), cellFromRow(styleRow, colProjectName));
            setCellValue(row, colAttr, rowData.getPersonnelAttribute(), cellFromRow(styleRow, colAttr));
            setCellValue(row, colManager, rowData.isProjectManager() ? "是" : "否", cellFromRow(styleRow, colManager));
            setCellValue(row, colMonths, joinMonths(rowData.getMonthlyHours().keySet()), cellFromRow(styleRow, colMonths));
        }
    }

    private void writeSummarySheet(Workbook workbook, ExportContext context) {
        Sheet sheet = ExcelTemplateUtils.findSheetByNameContains(workbook, "汇总");
        if (sheet == null) {
            return;
        }
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex < 0) {
            return;
        }
        Row headerRow = sheet.getRow(headerRowIndex);
        int dataStartRow = headerRowIndex + 1;
        int colSeq = findColumnIndexContains(headerRow, "序号", 0);
        int colEmpNo = findColumnIndexContains(headerRow, "工号", 1);
        int colName = findColumnIndexContains(headerRow, "姓名", 2);
        int colProjectCode = findColumnIndexContains(headerRow, "项目号", 3);

        Map<YearMonth, ColumnGroup> groupMap = new LinkedHashMap<>();
        for (YearMonth month : context.getMonths()) {
            ColumnGroup group = new ColumnGroup();
            group.salary = findColumnIndexContains(headerRow, month.getMonthValue() + "月工资", -1);
            group.social = findColumnIndexContains(headerRow, month.getMonthValue() + "月社保", -1);
            group.fund = findColumnIndexContains(headerRow, month.getMonthValue() + "月公积金", -1);
            group.hours = findColumnIndexContains(headerRow, month.getMonthValue() + "月研发工时", -1);
            groupMap.put(month, group);
        }
        int colSalaryTotal = findColumnIndexContains(headerRow, "工资合计", -1);
        int colSocialTotal = findColumnIndexContains(headerRow, "社保合计", -1);
        int colFundTotal = findColumnIndexContains(headerRow, "公积金合计", -1);
        int colHoursTotal = findColumnIndexContains(headerRow, "研发工时合计", -1);

        clearDataRows(sheet, dataStartRow, colSeq);
        Row styleRow = sheet.getRow(dataStartRow);
        int rowIndex = dataStartRow;
        int seq = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            Row row = ensureRow(sheet, rowIndex++, styleRow);
            setCellValue(row, colSeq, seq++, cellFromRow(styleRow, colSeq));
            setCellValue(row, colEmpNo, rowData.getEmployeeNo(), cellFromRow(styleRow, colEmpNo));
            setCellValue(row, colName, rowData.getEmployeeName(), cellFromRow(styleRow, colName));
            setCellValue(row, colProjectCode, rowData.getProjectCode(), cellFromRow(styleRow, colProjectCode));

            BigDecimal salaryTotal = BigDecimal.ZERO;
            BigDecimal socialTotal = BigDecimal.ZERO;
            BigDecimal fundTotal = BigDecimal.ZERO;
            BigDecimal hoursTotal = BigDecimal.ZERO;
            for (YearMonth month : context.getMonths()) {
                WageRow wage = rowData.getMonthlyWages().get(month);
                BigDecimal social = wage.socialTotal();
                ColumnGroup group = groupMap.get(month);
                if (group.salary >= 0) {
                    setNumericIfNotZero(row, group.salary, wage.getSalary(), cellFromRow(styleRow, group.salary));
                }
                if (group.social >= 0) {
                    setNumericIfNotZero(row, group.social, social, cellFromRow(styleRow, group.social));
                }
                if (group.fund >= 0) {
                    setNumericIfNotZero(row, group.fund, wage.getHousingFund(), cellFromRow(styleRow, group.fund));
                }
                if (group.hours >= 0) {
                    setNumericIfNotZero(row, group.hours, safe(wage.getRdHours()), cellFromRow(styleRow, group.hours));
                }
                salaryTotal = salaryTotal.add(wage.getSalary());
                socialTotal = socialTotal.add(social);
                fundTotal = fundTotal.add(wage.getHousingFund());
                hoursTotal = hoursTotal.add(safe(wage.getRdHours()));
            }
            if (colSalaryTotal >= 0) {
                setNumericIfNotZero(row, colSalaryTotal, salaryTotal, cellFromRow(styleRow, colSalaryTotal));
            }
            if (colSocialTotal >= 0) {
                setNumericIfNotZero(row, colSocialTotal, socialTotal, cellFromRow(styleRow, colSocialTotal));
            }
            if (colFundTotal >= 0) {
                setNumericIfNotZero(row, colFundTotal, fundTotal, cellFromRow(styleRow, colFundTotal));
            }
            if (colHoursTotal >= 0) {
                setNumericIfNotZero(row, colHoursTotal, hoursTotal, cellFromRow(styleRow, colHoursTotal));
            }
        }
    }

    private void writeMonthlySheet(Sheet sheet, ExportContext context, YearMonth month) {
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex < 0) {
            return;
        }
        Row headerRow = sheet.getRow(headerRowIndex);
        int dataStartRow = headerRowIndex + 1;
        int colSeq = findColumnIndexContains(headerRow, "序号", 0);
        int colEmpNo = findColumnIndexContains(headerRow, "工号", 1);
        int colName = findColumnIndexContains(headerRow, "姓名", 2);
        int colAttr = findColumnIndexContains(headerRow, "人员", 3);
        int colProjectCode = findColumnIndexContains(headerRow, "项目号", 4);
        int colManager = findColumnIndexContains(headerRow, "负责人", 5);
        int colSalary = findColumnIndexContains(headerRow, "应发工资", -1);
        int colPension = findColumnIndexContains(headerRow, "养老保险", -1);
        int colUnemployment = findColumnIndexContains(headerRow, "失业保险", -1);
        int colMedical = findColumnIndexContains(headerRow, "医疗保险", -1);
        int colMaternity = findColumnIndexContains(headerRow, "生育保险", -1);
        int colInjury = findColumnIndexContains(headerRow, "工伤保险", -1);
        int colFund = findColumnIndexContains(headerRow, "公积金", -1);
        int colTotalHours = findColumnIndexContains(headerRow, "总工时", -1);
        int colRdHours = findColumnIndexContains(headerRow, "研发工时", -1);
        int colRdSalary = findColumnIndexContains(headerRow, "研发工资", -1);

        clearDataRows(sheet, dataStartRow, colSeq);
        Row styleRow = sheet.getRow(dataStartRow);
        int rowIndex = dataStartRow;
        int seq = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            WageRow wage = rowData.getMonthlyWages().get(month);
            Row row = ensureRow(sheet, rowIndex++, styleRow);
            setCellValue(row, colSeq, seq++, cellFromRow(styleRow, colSeq));
            setCellValue(row, colEmpNo, rowData.getEmployeeNo(), cellFromRow(styleRow, colEmpNo));
            setCellValue(row, colName, rowData.getEmployeeName(), cellFromRow(styleRow, colName));
            setCellValue(row, colAttr, rowData.getPersonnelAttribute(), cellFromRow(styleRow, colAttr));
            setCellValue(row, colProjectCode, rowData.getProjectCode(), cellFromRow(styleRow, colProjectCode));
            if (colManager >= 0) {
                setCellValue(row, colManager, rowData.isProjectManager() ? "是" : "否", cellFromRow(styleRow, colManager));
            }
            if (colSalary >= 0) {
                setCellValue(row, colSalary, wage.getSalary(), cellFromRow(styleRow, colSalary));
            }
            if (colPension >= 0) {
                setCellValue(row, colPension, wage.getPension(), cellFromRow(styleRow, colPension));
            }
            if (colUnemployment >= 0) {
                setCellValue(row, colUnemployment, wage.getUnemployment(), cellFromRow(styleRow, colUnemployment));
            }
            if (colMedical >= 0) {
                setCellValue(row, colMedical, wage.getMedical(), cellFromRow(styleRow, colMedical));
            }
            if (colMaternity >= 0) {
                setCellValue(row, colMaternity, wage.getMaternity(), cellFromRow(styleRow, colMaternity));
            }
            if (colInjury >= 0) {
                setCellValue(row, colInjury, wage.getInjury(), cellFromRow(styleRow, colInjury));
            }
            if (colFund >= 0) {
                setCellValue(row, colFund, wage.getHousingFund(), cellFromRow(styleRow, colFund));
            }
            if (colTotalHours >= 0) {
                setCellValue(row, colTotalHours, safe(wage.getTotalHours()), cellFromRow(styleRow, colTotalHours));
            }
            if (colRdHours >= 0) {
                setCellValue(row, colRdHours, safe(wage.getRdHours()), cellFromRow(styleRow, colRdHours));
            }
            if (colRdSalary >= 0) {
                setCellValue(row, colRdSalary, wage.getSalary(), cellFromRow(styleRow, colRdSalary));
            }
        }
    }

    private void writeAttendanceSheet(Sheet sheet, ExportContext context, YearMonth month) {
        updateAttendanceTitle(sheet, context, month);
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex < 0) {
            return;
        }
        Row headerRow = sheet.getRow(headerRowIndex);
        int dataStartRow = headerRowIndex + 1;
        int colSeq = findColumnIndexContains(headerRow, "序号", 0);
        int colEmpNo = findColumnIndexContains(headerRow, "工号", 1);
        int colName = findColumnIndexContains(headerRow, "姓名", 2);
        int colProjectCode = findColumnIndexContains(headerRow, "项目号", 3);
        int colAttendanceDays = findColumnIndexContains(headerRow, "实际出勤", -1);
        int colTotalHours = findColumnIndexContains(headerRow, "总工时", -1);
        int colRdDays = findColumnIndexContains(headerRow, "研发天数", -1);
        int totalCol = findColumnIndexContains(headerRow, "合计", -1);

        int days = month.lengthOfMonth();
        List<Integer> dayCols = new ArrayList<>();
        for (Cell cell : headerRow) {
            String value = ExcelTemplateUtils.getCellString(cell);
            Integer day = parseDayHeader(value);
            if (day != null && day >= 1 && day <= 31) {
                dayCols.add(cell.getColumnIndex());
            }
        }
        dayCols.sort(Integer::compareTo);
        if (dayCols.size() > days) {
            dayCols = dayCols.subList(0, days);
        }

        clearDataRows(sheet, dataStartRow, colSeq);
        Row styleRow = sheet.getRow(dataStartRow);
        Map<String, Map<Integer, BigDecimal>> dailyMap = buildAttendanceMap(context.getAttendanceRecords(), month);
        int rowIndex = dataStartRow;
        int seq = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            Row row = ensureRow(sheet, rowIndex++, styleRow);
            setCellValue(row, colSeq, seq++, cellFromRow(styleRow, colSeq));
            setCellValue(row, colEmpNo, rowData.getEmployeeNo(), cellFromRow(styleRow, colEmpNo));
            setCellValue(row, colName, rowData.getEmployeeName(), cellFromRow(styleRow, colName));
            setCellValue(row, colProjectCode, rowData.getProjectCode(), cellFromRow(styleRow, colProjectCode));
            String key = employeeKey(rowData.getEmployeeNo(), rowData.getEmployeeName(), rowData.getProjectCode());
            Map<Integer, BigDecimal> dailyHours = dailyMap.getOrDefault(key, Map.of());
            BigDecimal total = BigDecimal.ZERO;
            int dayCount = 0;
            for (int i = 0; i < dayCols.size(); i++) {
                int day = i + 1;
                BigDecimal value = dailyHours.get(day);
                total = total.add(safe(value));
                if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
                    dayCount++;
                }
                setCellValue(row, dayCols.get(i), value == null ? "-" : value.stripTrailingZeros().toPlainString(), cellFromRow(styleRow, dayCols.get(i)));
            }
            if (colAttendanceDays >= 0) {
                setCellValue(row, colAttendanceDays, dayCount, cellFromRow(styleRow, colAttendanceDays));
            }
            if (colTotalHours >= 0) {
                setCellValue(row, colTotalHours, total, cellFromRow(styleRow, colTotalHours));
            }
            if (colRdDays >= 0) {
                setCellValue(row, colRdDays, dayCount, cellFromRow(styleRow, colRdDays));
            }
            if (totalCol >= 0) {
                setCellValue(row, totalCol, total, cellFromRow(styleRow, totalCol));
            }
        }
    }

    private Sheet resolveMonthlySheet(Workbook workbook, YearMonth month, boolean attendance) {
        String sheetName = month.getMonthValue() + "月" + (attendance ? "考勤" : "");
        Sheet existing = workbook.getSheet(sheetName);
        if (existing != null) {
            return existing;
        }
        Sheet template = findTemplateMonthSheet(workbook, attendance);
        if (template == null) {
            return workbook.createSheet(sheetName);
        }
        return ExcelTemplateUtils.cloneSheet(workbook, template, sheetName);
    }

    private Sheet findTemplateMonthSheet(Workbook workbook, boolean attendance) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String name = sheet.getSheetName();
            if (attendance) {
                if (name.contains("考勤")) {
                    return sheet;
                }
            } else {
                if (name.matches("\\d+月") && !name.contains("考勤")) {
                    return sheet;
                }
            }
        }
        return null;
    }

    private Map<String, Map<Integer, BigDecimal>> buildAttendanceMap(List<AttendanceRecord> attendanceRecords, YearMonth month) {
        Map<String, Map<Integer, BigDecimal>> result = new LinkedHashMap<>();
        for (AttendanceRecord record : attendanceRecords) {
            if (!YearMonth.from(record.getWorkDate()).equals(month)) {
                continue;
            }
            String key = employeeKey(record.getEmployeeNo(), record.getEmployeeName(), record.getProjectCode());
            result.computeIfAbsent(key, ignore -> new TreeMap<>())
                    .merge(record.getWorkDate().getDayOfMonth(), safe(record.getDurationHours()), BigDecimal::add);
        }
        return result;
    }

    private BigDecimal readNestedAmount(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return BigDecimal.ZERO;
        }
        return readDecimal(child.get("amount"));
    }

    private BigDecimal readDecimal(JsonNode node) {
        if (node == null || node.isNull() || !StringUtils.hasText(node.asText())) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(node.asText("0"));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String employeeKey(String employeeNo, String employeeName, String projectCode) {
        String normalizedNo = normalizeKeyPart(employeeNo);
        String normalizedProject = normalizeKeyPart(projectCode);
        if (StringUtils.hasText(normalizedNo) || StringUtils.hasText(normalizedProject)) {
            return normalizedNo + "::" + normalizedProject;
        }
        return normalizeKeyPart(employeeName);
    }

    private String normalizeKeyPart(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toUpperCase();
    }

    private String joinMonths(Collection<YearMonth> months) {
        return months.stream().sorted().map(item -> item.getMonthValue() + "月").reduce((left, right) -> left + "、" + right).orElse("");
    }

    private String displayPersonnelAttribute(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "研发人员";
        }
        return switch (raw) {
            case "research" -> "研发人员";
            case "external" -> "外聘研发人员";
            default -> raw;
        };
    }

    private List<EmployeeAnnualRow> sortedEmployees(Collection<EmployeeAnnualRow> employees) {
        return employees.stream()
                .sorted(Comparator.comparing(EmployeeAnnualRow::getProjectCode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(EmployeeAnnualRow::getEmployeeNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(EmployeeAnnualRow::getEmployeeName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private int findHeaderRow(Sheet sheet) {
        int headerRowIndex = ExcelTemplateUtils.findRowIndexByCellValue(sheet, "序号");
        if (headerRowIndex >= 0) {
            return headerRowIndex;
        }
        return ExcelTemplateUtils.findRowIndexByCellContains(sheet, "序号");
    }

    private int findColumnIndexContains(Row row, String keyword, int fallback) {
        if (row == null) {
            return fallback;
        }
        for (Cell cell : row) {
            String value = ExcelTemplateUtils.getCellString(cell);
            if (value != null && value.contains(keyword)) {
                return cell.getColumnIndex();
            }
        }
        return fallback;
    }

    private void clearDataRows(Sheet sheet, int startRow, int seqCol) {
        int lastRow = sheet.getLastRowNum();
        for (int i = startRow; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            if (seqCol >= 0) {
                Cell cell = row.getCell(seqCol);
                if (cell == null || ExcelTemplateUtils.getCellString(cell) == null) {
                    continue;
                }
            }
            for (Cell cell : row) {
                if (cell != null) {
                    cell.setBlank();
                }
            }
        }
    }

    private Row ensureRow(Sheet sheet, int rowIndex, Row styleRow) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            row = sheet.createRow(rowIndex);
        }
        if (styleRow != null) {
            for (Cell templateCell : styleRow) {
                int col = templateCell.getColumnIndex();
                Cell cell = row.getCell(col);
                if (cell == null) {
                    cell = row.createCell(col);
                }
                CellStyle style = templateCell.getCellStyle();
                if (style != null) {
                    cell.setCellStyle(style);
                }
            }
        }
        return row;
    }

    private void setCellValue(Row row, int columnIndex, Object value, Cell templateCell) {
        if (row == null || columnIndex < 0) {
            return;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        if (templateCell != null) {
            CellStyle style = templateCell.getCellStyle();
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private void setNumericIfNotZero(Row row, int columnIndex, BigDecimal value, Cell templateCell) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            setCellValue(row, columnIndex, null, templateCell);
            return;
        }
        setCellValue(row, columnIndex, value, templateCell);
    }

    private void removeSheetByNameContains(Workbook workbook, String keyword) {
        for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName() != null && sheet.getSheetName().contains(keyword)) {
                workbook.removeSheetAt(i);
            }
        }
    }

    private void pruneMonthSheets(Workbook workbook, List<YearMonth> months) {
        List<Integer> keepMonths = months.stream().map(YearMonth::getMonthValue).toList();
        for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
            Sheet sheet = workbook.getSheetAt(i);
            String name = sheet.getSheetName();
            Integer monthValue = extractMonth(name);
            if (monthValue == null) {
                continue;
            }
            if (!keepMonths.contains(monthValue)) {
                workbook.removeSheetAt(i);
            }
        }
    }

    private Integer extractMonth(String sheetName) {
        if (sheetName == null || !sheetName.contains("月")) {
            return null;
        }
        String digits = sheetName.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(digits);
            return value >= 1 && value <= 12 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Cell cellFromRow(Row row, int columnIndex) {
        if (row == null || columnIndex < 0) {
            return null;
        }
        return row.getCell(columnIndex);
    }

    private Integer parseDayHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ignore) {
            // continue
        }
        try {
            double numeric = Double.parseDouble(trimmed);
            int day = (int) Math.round(numeric);
            if (Math.abs(numeric - day) < 0.0001d) {
                return day;
            }
        } catch (NumberFormatException ignore) {
            // ignore
        }
        return null;
    }

    private void updateAttendanceTitle(Sheet sheet, ExportContext context, YearMonth month) {
        if (sheet == null || context == null || context.getCompany() == null) {
            return;
        }
        int titleRowIndex = ExcelTemplateUtils.findRowIndexByCellContains(sheet, "\u5de5\u65f6\u8bb0\u5f55\u8868");
        if (titleRowIndex < 0) {
            return;
        }
        Row row = sheet.getRow(titleRowIndex);
        if (row == null) {
            return;
        }
        String companyName = context.getCompany().getName();
        if (!StringUtils.hasText(companyName)) {
            return;
        }
        String title = companyName + month.getYear() + "\u5e74" + month.getMonthValue() + "\u6708\u7814\u53d1\u4eba\u5458\u5de5\u65f6\u8bb0\u5f55\u8868";
        for (Cell cell : row) {
            String value = ExcelTemplateUtils.getCellString(cell);
            if (value != null && value.contains("\u5de5\u65f6\u8bb0\u5f55\u8868")) {
                setCellValue(row, cell.getColumnIndex(), title, cell);
                return;
            }
        }
    }

    @Data
    @Builder
    private static class ExportContext {
        private Company company;
        private List<YearMonth> months;
        private Map<String, String> projectNameByCode;
        private Map<String, EmployeeAnnualRow> employees;
        private List<AttendanceRecord> attendanceRecords;
        private List<ProjectMonthlyData> monthlyDataList;
    }

    @Data
    @Builder
    private static class EmployeeAnnualRow {
        private String employeeNo;
        private String employeeName;
        private String projectCode;
        private String projectName;
        private String personnelAttribute;
        private boolean projectManager;
        private Map<YearMonth, BigDecimal> monthlyHours;
        private Map<YearMonth, WageRow> monthlyWages;
    }

    @Data
    @Builder
    private static class WageRow {
        private String employeeNo;
        private String employeeName;
        private String projectCode;
        private String personnelAttribute;
        private Boolean projectManager;
        private BigDecimal totalHours;
        private BigDecimal rdHours;
        private BigDecimal salary;
        private BigDecimal pension;
        private BigDecimal unemployment;
        private BigDecimal medical;
        private BigDecimal maternity;
        private BigDecimal injury;
        private BigDecimal housingFund;
        private BigDecimal externalLabor;

        static WageRow empty(String employeeNo, String employeeName, String projectCode, String personnelAttribute, boolean projectManager) {
            return WageRow.builder()
                    .employeeNo(employeeNo)
                    .employeeName(employeeName)
                    .projectCode(projectCode)
                    .personnelAttribute(personnelAttribute)
                    .projectManager(projectManager)
                    .totalHours(BigDecimal.ZERO)
                    .rdHours(BigDecimal.ZERO)
                    .salary(BigDecimal.ZERO)
                    .pension(BigDecimal.ZERO)
                    .unemployment(BigDecimal.ZERO)
                    .medical(BigDecimal.ZERO)
                    .maternity(BigDecimal.ZERO)
                    .injury(BigDecimal.ZERO)
                    .housingFund(BigDecimal.ZERO)
                    .externalLabor(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal socialTotal() {
            return value(pension).add(value(unemployment)).add(value(medical)).add(value(maternity)).add(value(injury));
        }

        private static BigDecimal value(BigDecimal input) {
            return input == null ? BigDecimal.ZERO : input;
        }
    }

    private static class ColumnGroup {
        private int salary = -1;
        private int social = -1;
        private int fund = -1;
        private int hours = -1;
    }
}
