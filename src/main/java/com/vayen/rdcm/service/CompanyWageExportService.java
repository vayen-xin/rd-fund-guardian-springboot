package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayen.rdcm.entity.AttendanceRecord;
import com.vayen.rdcm.entity.Company;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.AttendanceRecordMapper;
import com.vayen.rdcm.mapper.CompanyMapper;
import com.vayen.rdcm.mapper.ProjectMapper;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.security.CurrentUser;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    /**
     * 导出公司级研发工资明细表。
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

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeInstructionSheet(workbook, context);
            writeRosterSheet(workbook, context);
            writeSummarySheet(workbook, context);
            writeDiffSheet(workbook, context);
            for (YearMonth month : context.getMonths()) {
                writeMonthlySheet(workbook, context, month);
                writeAttendanceSheet(workbook, context, month);
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
        Map<String, String> projectNameByCode = projectMapper.selectList(projectWrapper).stream()
                .filter(item -> StringUtils.hasText(item.getCode()))
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getCode(), item.getProjectName()), LinkedHashMap::putAll);

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
            log.warn("解析工资导出 labor 数据失败，已跳过该月度记录");
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

    private void writeInstructionSheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("说明");
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        writeCell(sheet.createRow(0), 0, "研发工资明细表导出说明", titleStyle);
        writeCell(sheet.createRow(2), 0, "公司名称", textStyle);
        writeCell(sheet.getRow(2), 1, context.getCompany().getName(), textStyle);
        writeCell(sheet.createRow(3), 0, "导出范围", textStyle);
        writeCell(sheet.getRow(3), 1, context.getMonths().get(0) + " 至 " + context.getMonths().get(context.getMonths().size() - 1), textStyle);
        writeCell(sheet.createRow(4), 0, "说明", textStyle);
        writeCell(sheet.getRow(4), 1, "本表根据系统中的打卡记录、月度费用和项目数据生成，用于研发工资审计测试。", textStyle);
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 88 * 256);
    }

    private void writeRosterSheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("研发人员清单" + context.getEmployees().size());
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        String[] headers = {"序号", "工号", "姓名", "项目号", "项目名称", "人员属性", "是否项目负责人", "参与月份"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeCell(header, i, headers[i], headerStyle);
        }
        int rowIndex = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            Row row = sheet.createRow(rowIndex);
            writeCell(row, 0, rowIndex, textStyle);
            writeCell(row, 1, rowData.getEmployeeNo(), textStyle);
            writeCell(row, 2, rowData.getEmployeeName(), textStyle);
            writeCell(row, 3, rowData.getProjectCode(), textStyle);
            writeCell(row, 4, rowData.getProjectName(), textStyle);
            writeCell(row, 5, rowData.getPersonnelAttribute(), textStyle);
            writeCell(row, 6, rowData.isProjectManager() ? "是" : "否", textStyle);
            writeCell(row, 7, joinMonths(rowData.getMonthlyHours().keySet()), textStyle);
            rowIndex++;
        }
        autoSize(sheet, headers.length);
    }

    private void writeSummarySheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("汇总");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        Row header = sheet.createRow(0);
        String[] baseHeaders = {"序号", "工号", "姓名", "项目号"};
        int columnIndex = 0;
        for (String value : baseHeaders) {
            writeCell(header, columnIndex++, value, headerStyle);
        }
        for (YearMonth month : context.getMonths()) {
            writeCell(header, columnIndex++, month.getMonthValue() + "月工资", headerStyle);
            writeCell(header, columnIndex++, month.getMonthValue() + "月社保", headerStyle);
            writeCell(header, columnIndex++, month.getMonthValue() + "月公积金", headerStyle);
            writeCell(header, columnIndex++, month.getMonthValue() + "月研发工时", headerStyle);
        }
        writeCell(header, columnIndex++, "工资合计", headerStyle);
        writeCell(header, columnIndex++, "社保合计", headerStyle);
        writeCell(header, columnIndex++, "公积金合计", headerStyle);
        writeCell(header, columnIndex, "研发工时合计", headerStyle);

        int rowIndex = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            Row row = sheet.createRow(rowIndex);
            int cellIndex = 0;
            writeCell(row, cellIndex++, rowIndex, textStyle);
            writeCell(row, cellIndex++, rowData.getEmployeeNo(), textStyle);
            writeCell(row, cellIndex++, rowData.getEmployeeName(), textStyle);
            writeCell(row, cellIndex++, rowData.getProjectCode(), textStyle);
            BigDecimal salaryTotal = BigDecimal.ZERO;
            BigDecimal socialTotal = BigDecimal.ZERO;
            BigDecimal fundTotal = BigDecimal.ZERO;
            BigDecimal hoursTotal = BigDecimal.ZERO;
            for (YearMonth month : context.getMonths()) {
                WageRow wage = rowData.getMonthlyWages().get(month);
                BigDecimal social = wage.socialTotal();
                writeCell(row, cellIndex++, wage.getSalary().doubleValue(), textStyle);
                writeCell(row, cellIndex++, social.doubleValue(), textStyle);
                writeCell(row, cellIndex++, wage.getHousingFund().doubleValue(), textStyle);
                writeCell(row, cellIndex++, safe(wage.getRdHours()).doubleValue(), textStyle);
                salaryTotal = salaryTotal.add(wage.getSalary());
                socialTotal = socialTotal.add(social);
                fundTotal = fundTotal.add(wage.getHousingFund());
                hoursTotal = hoursTotal.add(safe(wage.getRdHours()));
            }
            writeCell(row, cellIndex++, salaryTotal.doubleValue(), textStyle);
            writeCell(row, cellIndex++, socialTotal.doubleValue(), textStyle);
            writeCell(row, cellIndex++, fundTotal.doubleValue(), textStyle);
            writeCell(row, cellIndex, hoursTotal.doubleValue(), textStyle);
            rowIndex++;
        }
        autoSize(sheet, columnIndex + 1);
    }

    private void writeDiffSheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("差异调整");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        String[] headers = {"序号", "工号", "姓名", "项目号", "计提工资", "实际工资", "差异", "说明"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeCell(header, i, headers[i], headerStyle);
        }
        int rowIndex = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            BigDecimal accrual = rowData.getMonthlyWages().values().stream().map(WageRow::getSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
            Row row = sheet.createRow(rowIndex);
            writeCell(row, 0, rowIndex, textStyle);
            writeCell(row, 1, rowData.getEmployeeNo(), textStyle);
            writeCell(row, 2, rowData.getEmployeeName(), textStyle);
            writeCell(row, 3, rowData.getProjectCode(), textStyle);
            writeCell(row, 4, accrual.doubleValue(), textStyle);
            writeCell(row, 5, accrual.doubleValue(), textStyle);
            writeCell(row, 6, 0D, textStyle);
            writeCell(row, 7, "当前测试数据未设置差异调整", textStyle);
            rowIndex++;
        }
        autoSize(sheet, headers.length);
    }

    private void writeMonthlySheet(XSSFWorkbook workbook, ExportContext context, YearMonth month) {
        Sheet sheet = workbook.createSheet(month.getMonthValue() + "月");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        String[] headers = {
                "序号", "工号", "姓名", "人员属性", "项目号", "是否项目负责人",
                "应发工资", "公司承担养老保险", "公司承担失业保险", "公司承担医疗保险",
                "公司承担生育保险", "公司承担工伤保险", "公司承担住房公积金",
                "总工时", "研发工时", "研发工资", "养老保险", "失业保险", "医疗保险", "生育保险", "工伤保险", "住房公积金"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            writeCell(header, i, headers[i], headerStyle);
        }
        int rowIndex = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            WageRow wage = rowData.getMonthlyWages().get(month);
            Row row = sheet.createRow(rowIndex);
            int cellIndex = 0;
            writeCell(row, cellIndex++, rowIndex, textStyle);
            writeCell(row, cellIndex++, rowData.getEmployeeNo(), textStyle);
            writeCell(row, cellIndex++, rowData.getEmployeeName(), textStyle);
            writeCell(row, cellIndex++, rowData.getPersonnelAttribute(), textStyle);
            writeCell(row, cellIndex++, rowData.getProjectCode(), textStyle);
            writeCell(row, cellIndex++, rowData.isProjectManager() ? "是" : "否", textStyle);
            writeCell(row, cellIndex++, wage.getSalary().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getPension().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getUnemployment().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getMedical().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getMaternity().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getInjury().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getHousingFund().doubleValue(), textStyle);
            writeCell(row, cellIndex++, safe(wage.getTotalHours()).doubleValue(), textStyle);
            writeCell(row, cellIndex++, safe(wage.getRdHours()).doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getSalary().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getPension().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getUnemployment().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getMedical().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getMaternity().doubleValue(), textStyle);
            writeCell(row, cellIndex++, wage.getInjury().doubleValue(), textStyle);
            writeCell(row, cellIndex, wage.getHousingFund().doubleValue(), textStyle);
            rowIndex++;
        }
        autoSize(sheet, headers.length);
    }

    private void writeAttendanceSheet(XSSFWorkbook workbook, ExportContext context, YearMonth month) {
        Sheet sheet = workbook.createSheet(month.getMonthValue() + "月考勤");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        Row header = sheet.createRow(0);
        int cellIndex = 0;
        writeCell(header, cellIndex++, "序号", headerStyle);
        writeCell(header, cellIndex++, "工号", headerStyle);
        writeCell(header, cellIndex++, "姓名", headerStyle);
        writeCell(header, cellIndex++, "项目号", headerStyle);
        int days = month.lengthOfMonth();
        for (int day = 1; day <= days; day++) {
            writeCell(header, cellIndex++, day, headerStyle);
        }
        writeCell(header, cellIndex, "合计(小时)", headerStyle);

        Map<String, Map<Integer, BigDecimal>> dailyMap = buildAttendanceMap(context.getAttendanceRecords(), month);
        int rowIndex = 1;
        for (EmployeeAnnualRow rowData : sortedEmployees(context.getEmployees().values())) {
            Row row = sheet.createRow(rowIndex);
            int colIndex = 0;
            writeCell(row, colIndex++, rowIndex, textStyle);
            writeCell(row, colIndex++, rowData.getEmployeeNo(), textStyle);
            writeCell(row, colIndex++, rowData.getEmployeeName(), textStyle);
            writeCell(row, colIndex++, rowData.getProjectCode(), textStyle);
            String key = employeeKey(rowData.getEmployeeNo(), rowData.getEmployeeName(), rowData.getProjectCode());
            Map<Integer, BigDecimal> dailyHours = dailyMap.getOrDefault(key, Map.of());
            BigDecimal total = BigDecimal.ZERO;
            for (int day = 1; day <= days; day++) {
                BigDecimal value = dailyHours.get(day);
                total = total.add(safe(value));
                writeCell(row, colIndex++, value == null ? "-" : value.stripTrailingZeros().toPlainString(), textStyle);
            }
            writeCell(row, colIndex, total.doubleValue(), textStyle);
            rowIndex++;
        }
        sheet.createFreezePane(4, 1);
        for (int i = 0; i < 4 + days + 1; i++) {
            sheet.setColumnWidth(i, i < 4 ? 14 * 256 : 5 * 256);
        }
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
        return (employeeNo == null ? "" : employeeNo) + "::" + (employeeName == null ? "" : employeeName) + "::" + (projectCode == null ? "" : projectCode);
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

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTextStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void writeCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 40 * 256));
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
}
