package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vayen.rdcm.config.AppUploadProperties;
import com.vayen.rdcm.dto.MonthlyDataDetailResponse;
import com.vayen.rdcm.entity.AttendanceRecord;
import com.vayen.rdcm.entity.Employee;
import com.vayen.rdcm.entity.ExpenseVoucher;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectEmployee;
import com.vayen.rdcm.entity.ProjectMonthlyData;
import com.vayen.rdcm.mapper.AttendanceRecordMapper;
import com.vayen.rdcm.mapper.EmployeeMapper;
import com.vayen.rdcm.mapper.ExpenseVoucherMapper;
import com.vayen.rdcm.mapper.ProjectMonthlyDataMapper;
import com.vayen.rdcm.security.CurrentUser;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
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
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditExportService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final List<String> LABOR_HEADERS = List.of("工资薪金", "社会保险费", "住房公积金", "外聘科技人员劳务费", "人工费用合计");

    private final ProjectService projectService;
    private final ProjectEmployeeService projectEmployeeService;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final ProjectMonthlyDataMapper projectMonthlyDataMapper;
    private final ExpenseVoucherMapper expenseVoucherMapper;
    private final EmployeeMapper employeeMapper;
    private final AppUploadProperties uploadProperties;

    /**
     * 导出项目年度审计 Excel。
     */
    public byte[] exportAuditWorkbook(Long projectId, Integer year, CurrentUser currentUser) {
        ExportContext context = loadContext(projectId, year, currentUser);
        log.info("公司 {} 用户 {} 导出项目审计Excel，projectId={}，year={}",
                currentUser.getCompanyId(), currentUser.getUsername(), projectId, year);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeInstructionSheet(workbook, context);
            writeEmployeeSheet(workbook, context);
            writeSummarySheet(workbook, context);
            for (int month = 1; month <= 12; month++) {
                writeMonthlyLaborSheet(workbook, context, month);
                writeMonthlyAttendanceSheet(workbook, context, month);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("生成审计 Excel 失败", ex);
        }
    }

    /**
     * 导出项目年度审计材料包。
     */
    public byte[] exportAuditPackage(Long projectId, Integer year, CurrentUser currentUser) {
        ExportContext context = loadContext(projectId, year, currentUser);
        log.info("公司 {} 用户 {} 导出项目审计材料包，projectId={}，year={}",
                currentUser.getCompanyId(), currentUser.getUsername(), projectId, year);
        byte[] workbookBytes = exportAuditWorkbook(projectId, year, currentUser);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addZipEntry(zipOutputStream, context.getWorkbookName(), workbookBytes);
            addVoucherEntries(zipOutputStream, context);
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("生成审计材料包失败", ex);
        }
    }

    private ExportContext loadContext(Long projectId, Integer year, CurrentUser currentUser) {
        if (year == null || year < 2000 || year > 2100) {
            throw new IllegalArgumentException("导出年份不合法");
        }
        Project project = projectService.getProjectById(projectId, currentUser);
        List<AttendanceRecord> attendanceRecords = loadAttendance(project, year, currentUser);
        List<ProjectMonthlyData> monthlyDataList = loadMonthlyData(projectId, year);
        List<ExpenseVoucher> vouchers = loadVouchers(projectId, year);
        Map<Long, Employee> employeeMap = loadProjectEmployees(projectId);
        List<AnnualEmployeeRow> annualEmployees = buildAnnualEmployees(project, attendanceRecords, monthlyDataList, employeeMap);
        Map<Integer, MonthlyBucket> monthlyBuckets = buildMonthlyBuckets(year, attendanceRecords, monthlyDataList, annualEmployees);
        return ExportContext.builder()
                .project(project)
                .year(year)
                .annualEmployees(annualEmployees)
                .monthlyBuckets(monthlyBuckets)
                .vouchers(vouchers)
                .workbookName("附件二-研发工资明细表-" + sanitizeFileName(project.getCode()) + "-" + year + ".xlsx")
                .build();
    }

    private List<AttendanceRecord> loadAttendance(Project project, Integer year, CurrentUser currentUser) {
        QueryWrapper<AttendanceRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("project_code", project.getCode())
                .ge("work_date", LocalDate.of(year, 1, 1))
                .le("work_date", LocalDate.of(year, 12, 31))
                .orderByAsc("work_date")
                .orderByAsc("employee_no");
        if (!currentUser.isAdmin()) {
            wrapper.eq("company_id", currentUser.getCompanyId());
        }
        return attendanceRecordMapper.selectList(wrapper);
    }

    private List<ProjectMonthlyData> loadMonthlyData(Long projectId, Integer year) {
        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .ge("work_month", LocalDate.of(year, 1, 1).atStartOfDay())
                .le("work_month", LocalDate.of(year, 12, 31).atTime(23, 59, 59))
                .orderByAsc("work_month");
        return projectMonthlyDataMapper.selectList(wrapper);
    }

    private List<ExpenseVoucher> loadVouchers(Long projectId, Integer year) {
        QueryWrapper<ExpenseVoucher> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .likeRight("`year_month`", year + "-")
                .orderByAsc("`year_month`")
                .orderByAsc("category")
                .orderByAsc("original_file_name");
        return expenseVoucherMapper.selectList(wrapper);
    }

    private Map<Long, Employee> loadProjectEmployees(Long projectId) {
        List<ProjectEmployee> relations = projectEmployeeService.getByProjectId(projectId);
        List<Long> employeeIds = relations.stream().map(ProjectEmployee::getEmployeeId).filter(Objects::nonNull).distinct().toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<Employee> wrapper = new QueryWrapper<>();
        wrapper.in("id", employeeIds);
        return employeeMapper.selectList(wrapper).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getId(), item), LinkedHashMap::putAll);
    }

    private List<AnnualEmployeeRow> buildAnnualEmployees(Project project,
                                                         List<AttendanceRecord> attendanceRecords,
                                                         List<ProjectMonthlyData> monthlyDataList,
                                                         Map<Long, Employee> employeeMap) {
        Map<String, AnnualEmployeeRow> rows = new LinkedHashMap<>();
        for (Employee employee : employeeMap.values()) {
            rows.put(employeeKey(employee.getEmployeeId(), employee.getName()), AnnualEmployeeRow.builder()
                    .employeeNo(employee.getEmployeeId())
                    .employeeName(employee.getName())
                    .department(employee.getDepartment())
                    .position(employee.getPosition())
                    .entryDate(employee.getEntryDate())
                    .projectCode(project.getCode())
                    .months(new LinkedHashSet<>())
                    .totalHours(BigDecimal.ZERO)
                    .build());
        }
        for (AttendanceRecord record : attendanceRecords) {
            AnnualEmployeeRow row = rows.computeIfAbsent(employeeKey(record.getEmployeeNo(), record.getEmployeeName()), ignore -> AnnualEmployeeRow.builder()
                    .employeeNo(record.getEmployeeNo())
                    .employeeName(record.getEmployeeName())
                    .projectCode(record.getProjectCode())
                    .months(new LinkedHashSet<>())
                    .totalHours(BigDecimal.ZERO)
                    .build());
            row.getMonths().add(record.getWorkDate().getMonthValue());
            row.setTotalHours(safe(row.getTotalHours()).add(safe(record.getDurationHours())));
        }
        for (ProjectMonthlyData monthlyData : monthlyDataList) {
            int month = monthlyData.getWorkMonth().getMonthValue();
            for (MonthlyDataDetailResponse.EmployeeItem employeeItem : parseMonthlyEmployees(monthlyData.getEmployeeData())) {
                AnnualEmployeeRow row = rows.computeIfAbsent(employeeKey(employeeItem.getEmployeeNo(), employeeItem.getName()), ignore -> AnnualEmployeeRow.builder()
                        .employeeNo(employeeItem.getEmployeeNo())
                        .employeeName(employeeItem.getName())
                        .department(employeeItem.getDepartment())
                        .projectCode(project.getCode())
                        .months(new LinkedHashSet<>())
                        .totalHours(BigDecimal.ZERO)
                        .build());
                row.getMonths().add(month);
                if (!StringUtils.hasText(row.getDepartment())) {
                    row.setDepartment(employeeItem.getDepartment());
                }
            }
        }
        return rows.values().stream()
                .sorted(Comparator.comparing(AnnualEmployeeRow::getEmployeeNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(AnnualEmployeeRow::getEmployeeName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private Map<Integer, MonthlyBucket> buildMonthlyBuckets(Integer year,
                                                            List<AttendanceRecord> attendanceRecords,
                                                            List<ProjectMonthlyData> monthlyDataList,
                                                            List<AnnualEmployeeRow> annualEmployees) {
        Map<Integer, MonthlyBucket> buckets = new TreeMap<>();
        for (int month = 1; month <= 12; month++) {
            buckets.put(month, MonthlyBucket.empty(annualEmployees));
        }
        for (AttendanceRecord record : attendanceRecords) {
            MonthlyBucket bucket = buckets.get(record.getWorkDate().getMonthValue());
            if (bucket == null) {
                continue;
            }
            EmployeeMonthRow row = bucket.getEmployeeRows().computeIfAbsent(employeeKey(record.getEmployeeNo(), record.getEmployeeName()), ignore -> EmployeeMonthRow.builder()
                    .employeeNo(record.getEmployeeNo())
                    .employeeName(record.getEmployeeName())
                    .projectCode(record.getProjectCode())
                    .dailyHours(new TreeMap<>())
                    .totalHours(BigDecimal.ZERO)
                    .build());
            row.getDailyHours().put(record.getWorkDate().getDayOfMonth(), safe(record.getDurationHours()));
            row.setTotalHours(safe(row.getTotalHours()).add(safe(record.getDurationHours())));
        }
        for (ProjectMonthlyData monthlyData : monthlyDataList) {
            MonthlyBucket bucket = buckets.get(monthlyData.getWorkMonth().getMonthValue());
            if (bucket == null) {
                continue;
            }
            bucket.setMonthlyData(monthlyData);
            bucket.setGrandTotal(monthlyData.getGrandTotal() == null ? BigDecimal.ZERO : BigDecimal.valueOf(monthlyData.getGrandTotal()));
            bucket.setLaborSummary(parseLaborSummary(monthlyData.getCostData()));
        }
        return buckets;
    }

    private LaborSummary parseLaborSummary(String costData) {
        LaborSummary summary = new LaborSummary();
        if (!StringUtils.hasText(costData)) {
            return summary;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(costData);
            JsonNode laborNode = root.get("labor");
            if (laborNode == null || laborNode.isNull()) {
                return summary;
            }
            if (laborNode.isArray()) {
                for (JsonNode item : laborNode) {
                    String employeeName = item.path("employee_name").asText("");
                    LaborBreakdown breakdown = summary.getByEmployee().computeIfAbsent(employeeName, ignore -> new LaborBreakdown());
                    breakdown.setSalary(breakdown.getSalary().add(readNestedAmount(item, "salary")));
                    breakdown.setSocialInsurance(breakdown.getSocialInsurance().add(readNestedAmount(item, "social_security")));
                    breakdown.setHousingFund(breakdown.getHousingFund().add(readNestedAmount(item, "housing_fund")));
                    breakdown.setExternalLabor(breakdown.getExternalLabor().add(readNestedAmount(item, "external_labor")));
                    summary.getCategoryTotals().merge("工资薪金", readNestedAmount(item, "salary"), BigDecimal::add);
                    summary.getCategoryTotals().merge("社会保险费", readNestedAmount(item, "social_security"), BigDecimal::add);
                    summary.getCategoryTotals().merge("住房公积金", readNestedAmount(item, "housing_fund"), BigDecimal::add);
                    summary.getCategoryTotals().merge("外聘科技人员劳务费", readNestedAmount(item, "external_labor"), BigDecimal::add);
                }
            } else if (laborNode.isObject()) {
                addLaborItems(summary, laborNode.get("systemItems"));
                addLaborItems(summary, laborNode.get("manualItems"));
            }
        } catch (Exception ex) {
            log.warn("解析人工费用导出数据失败，已降级为仅导出工时汇总");
        }
        return summary;
    }

    private void addLaborItems(LaborSummary summary, JsonNode itemsNode) {
        if (itemsNode == null || !itemsNode.isArray()) {
            return;
        }
        for (JsonNode item : itemsNode) {
            String itemCode = item.path("itemCode").asText("");
            String itemLabel = item.path("itemLabel").asText("");
            String name = item.path("name").asText("");
            BigDecimal amount = readFlatAmount(item);
            String label;
            if (StringUtils.hasText(itemLabel)) {
                label = itemLabel;
            } else if ("salary".equals(itemCode)) {
                label = "工资薪金";
            } else if ("housing_fund".equals(itemCode)) {
                label = "住房公积金";
            } else if (name.contains("社保")) {
                label = "社会保险费";
            } else if (name.contains("劳务")) {
                label = "外聘科技人员劳务费";
            } else {
                label = StringUtils.hasText(name) ? name : "人工费用";
            }
            summary.getCategoryTotals().merge(label, amount, BigDecimal::add);
        }
    }

    private List<MonthlyDataDetailResponse.EmployeeItem> parseMonthlyEmployees(String employeeData) {
        if (!StringUtils.hasText(employeeData)) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(employeeData, new TypeReference<List<MonthlyDataDetailResponse.EmployeeItem>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void writeInstructionSheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("说明");
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        writeCell(sheet.createRow(0), 0, "研发工资审计导出说明", titleStyle);
        writeCell(sheet.createRow(2), 0, "项目名称", textStyle);
        writeCell(sheet.getRow(2), 1, context.getProject().getProjectName(), textStyle);
        writeCell(sheet.createRow(3), 0, "项目编号", textStyle);
        writeCell(sheet.getRow(3), 1, context.getProject().getCode(), textStyle);
        writeCell(sheet.createRow(4), 0, "导出年份", textStyle);
        writeCell(sheet.getRow(4), 1, String.valueOf(context.getYear()), textStyle);
        writeCell(sheet.createRow(6), 0, "说明", textStyle);
        writeCell(sheet.getRow(6), 1, "本导出包依据系统内已归档的考勤、月度费用和凭证文件生成。", textStyle);
        writeCell(sheet.createRow(7), 1, "若工资拆分字段在系统中尚未单独维护，则相关单元格会保留为空或按汇总口径展示。", textStyle);
        writeCell(sheet.createRow(8), 1, "ZIP 包中会同步附带对应项目年度的凭证材料。", textStyle);
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 96 * 256);
    }

    private void writeEmployeeSheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("研发人员清单");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        List<String> headers = List.of("序号", "工号", "姓名", "部门", "岗位", "入职日期", "项目号", "参与月份", "年度工时(小时)");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            writeCell(header, i, headers.get(i), headerStyle);
        }
        int rowIndex = 1;
        for (AnnualEmployeeRow item : context.getAnnualEmployees()) {
            Row row = sheet.createRow(rowIndex);
            writeCell(row, 0, rowIndex, textStyle);
            writeCell(row, 1, item.getEmployeeNo(), textStyle);
            writeCell(row, 2, item.getEmployeeName(), textStyle);
            writeCell(row, 3, item.getDepartment(), textStyle);
            writeCell(row, 4, item.getPosition(), textStyle);
            writeCell(row, 5, item.getEntryDate(), textStyle);
            writeCell(row, 6, item.getProjectCode(), textStyle);
            writeCell(row, 7, joinMonths(item.getMonths()), textStyle);
            writeCell(row, 8, item.getTotalHours().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            rowIndex++;
        }
        autoSize(sheet, headers.size());
    }

    private void writeSummarySheet(XSSFWorkbook workbook, ExportContext context) {
        Sheet sheet = workbook.createSheet("汇总");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        List<String> headers = List.of("月份", "研发人数", "工时合计(小时)", "人工费用合计", "总费用合计", "状态");
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            writeCell(header, i, headers.get(i), headerStyle);
        }
        BigDecimal yearHours = BigDecimal.ZERO;
        BigDecimal yearLabor = BigDecimal.ZERO;
        BigDecimal yearTotal = BigDecimal.ZERO;
        int rowIndex = 1;
        for (int month = 1; month <= 12; month++) {
            MonthlyBucket bucket = context.getMonthlyBuckets().get(month);
            BigDecimal laborTotal = bucket.getLaborSummary().getCategoryTotals().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalHours = bucket.getEmployeeRows().values().stream().map(EmployeeMonthRow::getTotalHours).reduce(BigDecimal.ZERO, BigDecimal::add);
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, month + "月", textStyle);
            writeCell(row, 1, bucket.getEmployeeRows().size(), textStyle);
            writeCell(row, 2, totalHours.setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 3, laborTotal.setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 4, bucket.getGrandTotal().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 5, bucket.getMonthlyData() == null ? "未维护" : displayStatus(bucket.getMonthlyData().getStatus()), textStyle);
            yearHours = yearHours.add(totalHours);
            yearLabor = yearLabor.add(laborTotal);
            yearTotal = yearTotal.add(bucket.getGrandTotal());
        }
        Row totalRow = sheet.createRow(rowIndex);
        writeCell(totalRow, 0, "全年合计", headerStyle);
        writeCell(totalRow, 1, context.getAnnualEmployees().size(), headerStyle);
        writeCell(totalRow, 2, yearHours.setScale(2, RoundingMode.HALF_UP).doubleValue(), headerStyle);
        writeCell(totalRow, 3, yearLabor.setScale(2, RoundingMode.HALF_UP).doubleValue(), headerStyle);
        writeCell(totalRow, 4, yearTotal.setScale(2, RoundingMode.HALF_UP).doubleValue(), headerStyle);
        autoSize(sheet, headers.size());
    }

    private void writeMonthlyLaborSheet(XSSFWorkbook workbook, ExportContext context, int month) {
        Sheet sheet = workbook.createSheet(month + "月");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        MonthlyBucket bucket = context.getMonthlyBuckets().get(month);
        List<String> headers = new ArrayList<>(List.of("序号", "工号", "姓名", "部门", "项目号", "研发工时(小时)"));
        headers.addAll(LABOR_HEADERS);
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            writeCell(header, i, headers.get(i), headerStyle);
        }
        int rowIndex = 1;
        List<EmployeeMonthRow> rows = bucket.getEmployeeRows().values().stream()
                .sorted(Comparator.comparing(EmployeeMonthRow::getEmployeeNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(EmployeeMonthRow::getEmployeeName, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (EmployeeMonthRow item : rows) {
            LaborBreakdown laborBreakdown = bucket.getLaborSummary().getByEmployee().getOrDefault(item.getEmployeeName(), new LaborBreakdown());
            AnnualEmployeeRow annualEmployee = context.findAnnualEmployee(item.getEmployeeNo(), item.getEmployeeName());
            Row row = sheet.createRow(rowIndex);
            writeCell(row, 0, rowIndex, textStyle);
            writeCell(row, 1, item.getEmployeeNo(), textStyle);
            writeCell(row, 2, item.getEmployeeName(), textStyle);
            writeCell(row, 3, annualEmployee == null ? "" : annualEmployee.getDepartment(), textStyle);
            writeCell(row, 4, item.getProjectCode(), textStyle);
            writeCell(row, 5, item.getTotalHours().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 6, laborBreakdown.getSalary().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 7, laborBreakdown.getSocialInsurance().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 8, laborBreakdown.getHousingFund().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 9, laborBreakdown.getExternalLabor().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            writeCell(row, 10, laborBreakdown.total().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            rowIndex++;
        }
        rowIndex++;
        writeCell(sheet.createRow(rowIndex++), 0, "本月人工费用分类汇总", headerStyle);
        Row subHeader = sheet.createRow(rowIndex++);
        writeCell(subHeader, 0, "分类", headerStyle);
        writeCell(subHeader, 1, "金额", headerStyle);
        for (Map.Entry<String, BigDecimal> entry : bucket.getLaborSummary().getCategoryTotals().entrySet()) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, entry.getKey(), textStyle);
            writeCell(row, 1, entry.getValue().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
        }
        autoSize(sheet, 12);
    }

    private void writeMonthlyAttendanceSheet(XSSFWorkbook workbook, ExportContext context, int month) {
        Sheet sheet = workbook.createSheet(month + "月考勤");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);
        MonthlyBucket bucket = context.getMonthlyBuckets().get(month);
        int days = YearMonth.of(context.getYear(), month).lengthOfMonth();
        Row header = sheet.createRow(0);
        int colIndex = 0;
        writeCell(header, colIndex++, "序号", headerStyle);
        writeCell(header, colIndex++, "工号", headerStyle);
        writeCell(header, colIndex++, "姓名", headerStyle);
        writeCell(header, colIndex++, "项目号", headerStyle);
        for (int day = 1; day <= days; day++) {
            writeCell(header, colIndex++, day, headerStyle);
        }
        writeCell(header, colIndex, "合计(小时)", headerStyle);
        int rowIndex = 1;
        List<EmployeeMonthRow> rows = bucket.getEmployeeRows().values().stream()
                .sorted(Comparator.comparing(EmployeeMonthRow::getEmployeeNo, Comparator.nullsLast(String::compareTo))
                        .thenComparing(EmployeeMonthRow::getEmployeeName, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (EmployeeMonthRow item : rows) {
            Row row = sheet.createRow(rowIndex);
            int cellIndex = 0;
            writeCell(row, cellIndex++, rowIndex, textStyle);
            writeCell(row, cellIndex++, item.getEmployeeNo(), textStyle);
            writeCell(row, cellIndex++, item.getEmployeeName(), textStyle);
            writeCell(row, cellIndex++, item.getProjectCode(), textStyle);
            for (int day = 1; day <= days; day++) {
                BigDecimal hours = item.getDailyHours().get(day);
                writeCell(row, cellIndex++, hours == null ? "-" : hours.setScale(2, RoundingMode.HALF_UP).toPlainString(), textStyle);
            }
            writeCell(row, cellIndex, item.getTotalHours().setScale(2, RoundingMode.HALF_UP).doubleValue(), textStyle);
            rowIndex++;
        }
        sheet.createFreezePane(4, 1);
        for (int i = 0; i < 4 + days + 1; i++) {
            sheet.setColumnWidth(i, i < 4 ? 14 * 256 : 5 * 256);
        }
    }

    private void addVoucherEntries(ZipOutputStream zipOutputStream, ExportContext context) throws IOException {
        Path baseDir = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
        Set<String> entryNames = new LinkedHashSet<>();
        for (ExpenseVoucher voucher : context.getVouchers()) {
            Path filePath = baseDir.resolve(voucher.getRelativePath()).normalize();
            if (!Files.exists(filePath)) {
                continue;
            }
            String entryName = uniqueEntryName(entryNames,
                    "审计证明材料/" + voucher.getYearMonth() + "/" + voucher.getCategory() + "/" + sanitizeFileName(voucher.getOriginalFileName()));
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            Files.copy(filePath, zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    private void addZipEntry(ZipOutputStream zipOutputStream, String fileName, byte[] content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(fileName));
        zipOutputStream.write(content);
        zipOutputStream.closeEntry();
    }

    private String uniqueEntryName(Set<String> entryNames, String entryName) {
        if (entryNames.add(entryName)) {
            return entryName;
        }
        int index = 2;
        String suffixName = entryName;
        int dotIndex = entryName.lastIndexOf('.');
        while (!entryNames.add(suffixName)) {
            suffixName = dotIndex > 0
                    ? entryName.substring(0, dotIndex) + "(" + index + ")" + entryName.substring(dotIndex)
                    : entryName + "(" + index + ")";
            index++;
        }
        return suffixName;
    }

    private BigDecimal readNestedAmount(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull() || child.get("amount") == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(child.get("amount").asText("0"));
    }

    private BigDecimal readFlatAmount(JsonNode node) {
        JsonNode amount = node.get("amount");
        return amount == null || amount.isNull() ? BigDecimal.ZERO : new BigDecimal(amount.asText("0"));
    }

    private String employeeKey(String employeeNo, String employeeName) {
        return (employeeNo == null ? "" : employeeNo) + "::" + (employeeName == null ? "" : employeeName);
    }

    private String joinMonths(Collection<Integer> months) {
        return months.stream().sorted().map(item -> item + "月").reduce((left, right) -> left + "、" + right).orElse("");
    }

    private String displayStatus(String status) {
        return switch (status == null ? "" : status) {
            case "draft" -> "编辑中";
            case "finalized" -> "待结算";
            case "settled" -> "已结算";
            default -> status;
        };
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String sanitizeFileName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "未命名";
        }
        return raw.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
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
        style.setFillForegroundColor((short) 22);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
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
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 50 * 256));
        }
    }

    @Data
    @Builder
    private static class ExportContext {
        private Project project;
        private Integer year;
        private List<AnnualEmployeeRow> annualEmployees;
        private Map<Integer, MonthlyBucket> monthlyBuckets;
        private List<ExpenseVoucher> vouchers;
        private String workbookName;

        private AnnualEmployeeRow findAnnualEmployee(String employeeNo, String employeeName) {
            return annualEmployees.stream()
                    .filter(item -> Objects.equals(item.getEmployeeNo(), employeeNo) && Objects.equals(item.getEmployeeName(), employeeName))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Data
    @Builder
    private static class AnnualEmployeeRow {
        private String employeeNo;
        private String employeeName;
        private String department;
        private String position;
        private String entryDate;
        private String projectCode;
        private Set<Integer> months;
        private BigDecimal totalHours;
    }

    @Data
    @Builder
    private static class EmployeeMonthRow {
        private String employeeNo;
        private String employeeName;
        private String projectCode;
        private Map<Integer, BigDecimal> dailyHours;
        private BigDecimal totalHours;
    }

    @Data
    @Builder
    private static class MonthlyBucket {
        private ProjectMonthlyData monthlyData;
        private Map<String, EmployeeMonthRow> employeeRows;
        private LaborSummary laborSummary;
        private BigDecimal grandTotal;

        private static MonthlyBucket empty(List<AnnualEmployeeRow> annualEmployees) {
            Map<String, EmployeeMonthRow> rows = new LinkedHashMap<>();
            for (AnnualEmployeeRow employee : annualEmployees) {
                rows.put((employee.getEmployeeNo() == null ? "" : employee.getEmployeeNo()) + "::" + employee.getEmployeeName(),
                        EmployeeMonthRow.builder()
                                .employeeNo(employee.getEmployeeNo())
                                .employeeName(employee.getEmployeeName())
                                .projectCode(employee.getProjectCode())
                                .dailyHours(new TreeMap<>())
                                .totalHours(BigDecimal.ZERO)
                                .build());
            }
            return MonthlyBucket.builder()
                    .employeeRows(rows)
                    .laborSummary(new LaborSummary())
                    .grandTotal(BigDecimal.ZERO)
                    .build();
        }
    }

    @Data
    private static class LaborSummary {
        private Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        private Map<String, LaborBreakdown> byEmployee = new LinkedHashMap<>();
    }

    @Data
    private static class LaborBreakdown {
        private BigDecimal salary = BigDecimal.ZERO;
        private BigDecimal socialInsurance = BigDecimal.ZERO;
        private BigDecimal housingFund = BigDecimal.ZERO;
        private BigDecimal externalLabor = BigDecimal.ZERO;

        private BigDecimal total() {
            return salary.add(socialInsurance).add(housingFund).add(externalLabor);
        }
    }
}
