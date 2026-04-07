package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectMonthlyData;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectLedgerExportService {

    private final ProjectService projectService;
    private final ProjectMonthlyDataMapper projectMonthlyDataMapper;

    /**
     * 导出项目级研发支出辅助账。
     */
    public byte[] exportLedger(Long projectId, YearMonth startMonth, YearMonth endMonth, CurrentUser currentUser) {
        if (startMonth == null || endMonth == null || startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("导出时间范围不合法");
        }
        Project project = projectService.getProjectById(projectId, currentUser);
        if (!"ended".equals(project.getStatus()) && !"settled".equals(project.getStatus())) {
            throw new IllegalArgumentException("项目未结束，暂不支持导出研发支出辅助账");
        }

        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .ge("work_month", startMonth.atDay(1).atStartOfDay())
                .le("work_month", endMonth.atEndOfMonth().atTime(23, 59, 59))
                .orderByAsc("work_month");
        List<ProjectMonthlyData> monthlyDataList = projectMonthlyDataMapper.selectList(wrapper);
        LedgerContext context = buildContext(project, startMonth, endMonth, monthlyDataList);
        log.info("用户 {} 导出项目辅助账，projectId={}，period={}~{}", currentUser.getUsername(), projectId, startMonth, endMonth);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeLedgerSheet(workbook, context);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("导出研发支出辅助账失败", ex);
        }
    }

    private LedgerContext buildContext(Project project, YearMonth startMonth, YearMonth endMonth, List<ProjectMonthlyData> monthlyDataList) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        List<LedgerRow> rows = new ArrayList<>();
        for (YearMonth month : months) {
            ProjectMonthlyData monthlyData = monthlyDataList.stream()
                    .filter(item -> YearMonth.from(item.getWorkMonth()).equals(month))
                    .findFirst()
                    .orElse(null);
            if (monthlyData == null) {
                continue;
            }
            rows.add(buildLedgerRow(monthlyData));
        }
        return LedgerContext.builder()
                .project(project)
                .startMonth(startMonth)
                .endMonth(endMonth)
                .rows(rows)
                .build();
    }

    private LedgerRow buildLedgerRow(ProjectMonthlyData monthlyData) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put("1.1", decimal(monthlyData.getLaborTotal()));
        values.put("1.2", decimal(monthlyData.getLaborTotal()).equals(BigDecimal.ZERO) ? BigDecimal.ZERO : BigDecimal.ZERO);
        values.put("1.3", decimal(monthlyData.getOutsourcedTotal()));
        values.put("2.1", decimal(monthlyData.getDirectMaterialTotal()));
        values.put("2.2", decimal(monthlyData.getDirectFuelTotal()));
        values.put("2.3", BigDecimal.ZERO);
        values.put("2.4", BigDecimal.ZERO);
        values.put("2.5", BigDecimal.ZERO);
        values.put("2.6", BigDecimal.ZERO);
        values.put("2.7", BigDecimal.ZERO);
        values.put("2.8", decimal(monthlyData.getDirectRentalTotal()));
        values.put("3.1", BigDecimal.ZERO);
        values.put("3.2", decimal(monthlyData.getDepreciationTotal()));
        values.put("4.1", BigDecimal.ZERO);
        values.put("4.2", BigDecimal.ZERO);
        values.put("4.3", decimal(monthlyData.getAmortizationTotal()));
        values.put("5.1", decimal(monthlyData.getDesignTotal()));
        values.put("5.2", BigDecimal.ZERO);
        values.put("5.3", BigDecimal.ZERO);
        values.put("5.4", decimal(monthlyData.getCommissioningTotal()));
        values.put("6.1", BigDecimal.ZERO);
        values.put("6.2", BigDecimal.ZERO);
        values.put("6.3", decimal(monthlyData.getOtherTotal()));
        BigDecimal total = values.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return LedgerRow.builder()
                .month(YearMonth.from(monthlyData.getWorkMonth()))
                .voucherNo("SYS-" + YearMonth.from(monthlyData.getWorkMonth()).toString().replace("-", ""))
                .summary(YearMonth.from(monthlyData.getWorkMonth()).getMonthValue() + "月研发支出归集")
                .amounts(values)
                .total(total)
                .build();
    }

    private void writeLedgerSheet(XSSFWorkbook workbook, LedgerContext context) {
        Sheet sheet = workbook.createSheet("Sheet1");
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle textStyle = createTextStyle(workbook);

        writeCell(sheet.createRow(0), 0, "附件1", textStyle);
        writeCell(sheet.createRow(1), 0, "自主研发“研发支出”辅助账", titleStyle);

        Row infoRow = sheet.createRow(2);
        writeCell(infoRow, 0, "项目名称：", textStyle);
        writeCell(infoRow, 1, context.getProject().getProjectName(), textStyle);
        writeCell(infoRow, 4, "项目编号：" + context.getProject().getCode(), textStyle);
        writeCell(infoRow, 8, "资本化、费用化支出选项：", textStyle);
        writeCell(infoRow, 12, "○资本化", textStyle);
        writeCell(infoRow, 13, "◎费用化", textStyle);
        writeCell(infoRow, 15, "项目实施状态：", textStyle);
        writeCell(infoRow, 16, "已结束", textStyle);

        Row titleRow = sheet.createRow(3);
        writeCell(titleRow, 0, context.getStartMonth().getYear() + "年", headerStyle);
        writeCell(titleRow, 2, "凭证", headerStyle);
        writeCell(titleRow, 3, "摘要", headerStyle);
        writeCell(titleRow, 4, "借方金额", headerStyle);
        writeCell(titleRow, 5, "贷方金额", headerStyle);
        writeCell(titleRow, 6, "借或贷", headerStyle);
        writeCell(titleRow, 7, "余额", headerStyle);
        writeCell(titleRow, 8, "费用明细（借方）", headerStyle);

        Row row4 = sheet.createRow(4);
        writeCell(row4, 8, "一、人员人工费用", headerStyle);
        writeCell(row4, 11, "二、直接投入费用", headerStyle);
        writeCell(row4, 19, "三、折旧费用", headerStyle);
        writeCell(row4, 21, "四、无形资产摊销", headerStyle);
        writeCell(row4, 24, "五、新产品设计费等", headerStyle);
        writeCell(row4, 28, "六、其他相关费用", headerStyle);

        Row row5 = sheet.createRow(5);
        writeCell(row5, 8, "直接从事研发活动人员", headerStyle);
        writeCell(row5, 10, "外聘研发人员的劳务费用", headerStyle);
        writeCell(row5, 11, "研发活动直接消耗", headerStyle);
        writeCell(row5, 14, "模具、工艺装备开发及制造费", headerStyle);
        writeCell(row5, 15, "样品、样机及测试手段购置费", headerStyle);
        writeCell(row5, 16, "检验费", headerStyle);
        writeCell(row5, 17, "运行维护、调整、检验、维修等费用", headerStyle);
        writeCell(row5, 18, "经营租赁方式租入设备租赁费", headerStyle);
        writeCell(row5, 19, "仪器折旧费", headerStyle);
        writeCell(row5, 20, "设备折旧费", headerStyle);
        writeCell(row5, 21, "软件摊销费用", headerStyle);
        writeCell(row5, 22, "专利权摊销费用", headerStyle);
        writeCell(row5, 23, "非专利技术摊销费用", headerStyle);
        writeCell(row5, 24, "新产品设计费", headerStyle);
        writeCell(row5, 25, "新工艺规程制定费", headerStyle);
        writeCell(row5, 26, "临床试验费", headerStyle);
        writeCell(row5, 27, "现场试验费", headerStyle);
        writeCell(row5, 28, "知识产权申请、注册、代理费", headerStyle);
        writeCell(row5, 29, "福利费及补充保险", headerStyle);
        writeCell(row5, 30, "其他相关费用", headerStyle);

        Row row6 = sheet.createRow(6);
        writeCell(row6, 0, "月", headerStyle);
        writeCell(row6, 1, "日", headerStyle);
        writeCell(row6, 2, "号数", headerStyle);
        writeCell(row6, 8, "工资薪金", headerStyle);
        writeCell(row6, 9, "五险一金", headerStyle);
        writeCell(row6, 11, "材料", headerStyle);
        writeCell(row6, 12, "燃料", headerStyle);
        writeCell(row6, 13, "动力费用", headerStyle);

        Row row7 = sheet.createRow(7);
        writeCell(row7, 3, "序号", headerStyle);
        String[] codes = {"1.1","1.2","1.3","2.1","2.2","2.3","2.4","2.5","2.6","2.7","2.8","3.1","3.2","4.1","4.2","4.3","5.1","5.2","5.3","5.4","6.1","6.2","6.3","合计"};
        for (int i = 0; i < codes.length; i++) {
            writeCell(row7, 8 + i, codes[i], headerStyle);
        }

        Row opening = sheet.createRow(8);
        writeCell(opening, 3, "期初余额", textStyle);

        BigDecimal running = BigDecimal.ZERO;
        int rowIndex = 9;
        int sequence = 1;
        for (LedgerRow ledgerRow : context.getRows()) {
            Row row = sheet.createRow(rowIndex++);
            writeCell(row, 0, ledgerRow.getMonth().getMonthValue(), textStyle);
            writeCell(row, 1, ledgerRow.getMonth().atEndOfMonth().getDayOfMonth(), textStyle);
            writeCell(row, 2, ledgerRow.getVoucherNo(), textStyle);
            writeCell(row, 3, ledgerRow.getSummary(), textStyle);
            writeCell(row, 4, ledgerRow.getTotal().doubleValue(), textStyle);
            writeCell(row, 5, ledgerRow.getTotal().doubleValue(), textStyle);
            writeCell(row, 6, "", textStyle);
            running = running.add(ledgerRow.getTotal());
            writeCell(row, 7, running.doubleValue(), textStyle);
            int col = 8;
            writeCell(row, col++, sequence++, textStyle);
            for (String code : new String[]{"1.1","1.2","1.3","2.1","2.2","2.3","2.4","2.5","2.6","2.7","2.8","3.1","3.2","4.1","4.2","4.3","5.1","5.2","5.3","5.4","6.1","6.2","6.3"}) {
                writeCell(row, col++, ledgerRow.getAmounts().getOrDefault(code, BigDecimal.ZERO).doubleValue(), textStyle);
            }
            writeCell(row, col, ledgerRow.getTotal().doubleValue(), textStyle);
        }

        Row ending = sheet.createRow(rowIndex + 1);
        writeCell(ending, 3, "期末余额", textStyle);
        writeCell(ending, 7, running.doubleValue(), textStyle);

        for (int i = 0; i < 32; i++) {
            sheet.setColumnWidth(i, i < 8 ? 14 * 256 : 10 * 256);
        }
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
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

    @Data
    @Builder
    private static class LedgerContext {
        private Project project;
        private YearMonth startMonth;
        private YearMonth endMonth;
        private List<LedgerRow> rows;
    }

    @Data
    @Builder
    private static class LedgerRow {
        private YearMonth month;
        private String voucherNo;
        private String summary;
        private Map<String, BigDecimal> amounts;
        private BigDecimal total;
    }
}
