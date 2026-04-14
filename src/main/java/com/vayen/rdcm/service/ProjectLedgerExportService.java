package com.vayen.rdcm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vayen.rdcm.config.AuditTemplateProperties;
import com.vayen.rdcm.entity.Project;
import com.vayen.rdcm.entity.ProjectMonthlyData;
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
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
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
    private final AuditTemplateProperties auditTemplateProperties;

    /**
     * \u5bfc\u51fa\u9879\u76ee\u7ea7\u7814\u53d1\u652f\u51fa\u8f85\u52a9\u8d26
     */
    public byte[] exportLedger(Long projectId, YearMonth startMonth, YearMonth endMonth, CurrentUser currentUser) {
        if (startMonth == null || endMonth == null || startMonth.isAfter(endMonth)) {
            throw new IllegalArgumentException("\u5bfc\u51fa\u65f6\u95f4\u8303\u56f4\u4e0d\u5408\u6cd5");
        }
        Project project = projectService.getProjectById(projectId, currentUser);
        if (!"ended".equals(project.getStatus()) && !"settled".equals(project.getStatus())) {
            throw new IllegalArgumentException("\u9879\u76ee\u672a\u7ed3\u675f\uff0c\u6682\u4e0d\u652f\u6301\u5bfc\u51fa\u7814\u53d1\u652f\u51fa\u8f85\u52a9\u8d26");
        }

        QueryWrapper<ProjectMonthlyData> wrapper = new QueryWrapper<>();
        wrapper.eq("project_id", projectId)
                .ge("work_month", startMonth.atDay(1).atStartOfDay())
                .le("work_month", endMonth.atEndOfMonth().atTime(23, 59, 59))
                .orderByAsc("work_month");
        List<ProjectMonthlyData> monthlyDataList = projectMonthlyDataMapper.selectList(wrapper);
        LedgerContext context = buildContext(project, startMonth, endMonth, monthlyDataList);
        log.info("\u7528\u6237 {} \u5bfc\u51fa\u9879\u76ee\u8f85\u52a9\u8d26\uff0cprojectId={}，period={}~{}",
                currentUser.getUsername(), projectId, startMonth, endMonth);

        Workbook workbook = ExcelTemplateUtils.loadTemplate(auditTemplateProperties.getProjectLedgerPath());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            writeLedgerSheet(workbook, context);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("\u5bfc\u51fa\u7814\u53d1\u652f\u51fa\u8f85\u52a9\u8d26\u5931\u8d25", ex);
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
        values.put("1.2", BigDecimal.ZERO);
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
                .summary(YearMonth.from(monthlyData.getWorkMonth()).getMonthValue() + "\u6708\u7814\u53d1\u652f\u51fa\u5f52\u96c6")
                .amounts(values)
                .total(total)
                .build();
    }

    private void writeLedgerSheet(Workbook workbook, LedgerContext context) {
        Sheet sheet = workbook.getSheet("Sheet1");
        if (sheet == null) {
            sheet = workbook.getSheetAt(0);
        }
        if (sheet == null) {
            sheet = workbook.createSheet("Sheet1");
        }

        fillProjectMeta(sheet, context);

        int openingRowIndex = findRowIndexByLabel(sheet, "\u671f\u521d\u4f59\u989d");
        int dataStartRow = openingRowIndex >= 0 ? openingRowIndex + 1 : 9;
        int endingRowIndex = findRowIndexByLabel(sheet, "\u671f\u672b\u4f59\u989d");

        int codeRowIndex = findRowIndexByLabel(sheet, "1.1");
        Row codeRow = codeRowIndex >= 0 ? sheet.getRow(codeRowIndex) : null;
        Map<String, Integer> codeColumns = buildCodeColumnMap(codeRow);
        int totalColumn = findColumnIndexContains(codeRow, "\u5408\u8ba1", -1);
        int seqColumn = findColumnIndexContains(codeRow, "\u5e8f\u53f7", 3);

        Row headerRow = findRowByLabel(sheet, "\u53f7\u6570");
        int colMonth = findColumnIndexContains(headerRow, "\u6708", 0);
        int colDay = findColumnIndexContains(headerRow, "\u65e5", 1);
        int colVoucher = findColumnIndexContains(headerRow, "\u53f7\u6570", 2);
        int colSummary = findColumnIndexContains(headerRow, "\u6458\u8981", 3);
        int colDebit = findColumnIndexContains(headerRow, "\u501f\u65b9\u91d1\u989d", 4);
        int colCredit = findColumnIndexContains(headerRow, "\u8d37\u65b9\u91d1\u989d", 5);
        int colBalance = findColumnIndexContains(headerRow, "\u4f59\u989d", 7);

        int clearEndRow = endingRowIndex > 0 ? endingRowIndex - 1 : sheet.getLastRowNum();
        int existingCapacity = endingRowIndex > dataStartRow ? endingRowIndex - dataStartRow : 0;
        int requiredRows = context.getRows().size();
        if (requiredRows > existingCapacity && endingRowIndex > 0) {
            int shiftBy = requiredRows - existingCapacity;
            sheet.shiftRows(endingRowIndex, sheet.getLastRowNum(), shiftBy, true, false);
            endingRowIndex += shiftBy;
            clearEndRow = endingRowIndex - 1;
        }
        clearDataRows(sheet, dataStartRow, clearEndRow, seqColumn);
        Row templateRow = sheet.getRow(dataStartRow);

        BigDecimal running = BigDecimal.ZERO;
        int rowIndex = dataStartRow;
        int sequence = 1;
        for (LedgerRow ledgerRow : context.getRows()) {
            Row row = ensureRow(sheet, rowIndex++, templateRow);
            setCellValue(row, colMonth, ledgerRow.getMonth().getMonthValue(), cellFromRow(templateRow, colMonth));
            setCellValue(row, colDay, ledgerRow.getMonth().atEndOfMonth().getDayOfMonth(), cellFromRow(templateRow, colDay));
            setCellValue(row, colVoucher, ledgerRow.getVoucherNo(), cellFromRow(templateRow, colVoucher));
            setCellValue(row, colSummary, ledgerRow.getSummary(), cellFromRow(templateRow, colSummary));
            setCellValue(row, colDebit, ledgerRow.getTotal(), cellFromRow(templateRow, colDebit));
            setCellValue(row, colCredit, ledgerRow.getTotal(), cellFromRow(templateRow, colCredit));
            running = running.add(ledgerRow.getTotal());
            setCellValue(row, colBalance, running, cellFromRow(templateRow, colBalance));
            if (seqColumn >= 0) {
                setCellValue(row, seqColumn, sequence++, cellFromRow(templateRow, seqColumn));
            }
            for (Map.Entry<String, Integer> entry : codeColumns.entrySet()) {
                String code = entry.getKey();
                Integer col = entry.getValue();
                setCellValue(row, col, ledgerRow.getAmounts().getOrDefault(code, BigDecimal.ZERO), cellFromRow(templateRow, col));
            }
            if (totalColumn >= 0) {
                setCellValue(row, totalColumn, ledgerRow.getTotal(), cellFromRow(templateRow, totalColumn));
            }
        }

        if (openingRowIndex >= 0) {
            Row opening = sheet.getRow(openingRowIndex);
            if (opening != null) {
                setCellValue(opening, colSummary, "\u671f\u521d\u4f59\u989d", cellFromRow(opening, colSummary));
            }
        }
        int lastDataRow = dataStartRow + Math.max(context.getRows().size() - 1, 0);
        int resolvedEndingRow = endingRowIndex >= 0 ? endingRowIndex : lastDataRow + 1;
        if (resolvedEndingRow <= lastDataRow) {
            resolvedEndingRow = lastDataRow + 1;
        }
        Row ending = ensureRow(sheet, resolvedEndingRow, templateRow);
        setCellValue(ending, colSummary, "\u671f\u672b\u4f59\u989d", cellFromRow(templateRow, colSummary));
        setCellValue(ending, colBalance, running, cellFromRow(templateRow, colBalance));

        adjustVoucherColumnWidth(sheet, colVoucher);
    }

    private BigDecimal decimal(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private void fillProjectMeta(Sheet sheet, LedgerContext context) {
        fillLabelValue(sheet, "\u9879\u76ee\u540d\u79f0", context.getProject().getProjectName());
        fillLabelValue(sheet, "\u9879\u76ee\u7f16\u53f7", context.getProject().getCode());
        String statusText = mapProjectStatus(context.getProject().getStatus());
        fillLabelValue(sheet, "\u9879\u76ee\u5b9e\u65bd\u72b6\u6001", statusText);
        markProjectStatusOptions(sheet, statusText);
    }

    private String mapProjectStatus(String status) {
        if (status == null) {
            return "\u672a\u5b9a\u4e49";
        }
        return switch (status) {
            case "draft" -> "\u8349\u7a3f";
            case "ongoing" -> "\u8fdb\u884c\u4e2d";
            case "ended", "settled" -> "\u5df2\u5b8c\u6210";
            default -> status;
        };
    }

    private void markProjectStatusOptions(Sheet sheet, String statusText) {
        int rowIndex = findRowIndexByLabel(sheet, "\u9879\u76ee\u5b9e\u65bd\u72b6\u6001");
        if (rowIndex < 0) {
            return;
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return;
        }
        int colIncomplete = findColumnIndexContains(row, "\u672a\u5b8c\u6210", -1);
        int colComplete = findColumnIndexContains(row, "\u5df2\u5b8c\u6210", -1);
        if (colIncomplete >= 0) {
            setCellValue(row, colIncomplete, "\u672a\u5b8c\u6210".equals(statusText) ? "\u25ce\u672a\u5b8c\u6210" : "\u25cb\u672a\u5b8c\u6210", cellFromRow(row, colIncomplete));
        }
        if (colComplete >= 0) {
            setCellValue(row, colComplete, "\u5df2\u5b8c\u6210".equals(statusText) ? "\u25ce\u5df2\u5b8c\u6210" : "\u25cb\u5df2\u5b8c\u6210", cellFromRow(row, colComplete));
        }
    }

    private void clearDataRows(Sheet sheet, int startRow, int endRow, int seqCol) {
        int lastRow = Math.min(sheet.getLastRowNum(), endRow);
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

    private int findRowIndexByLabel(Sheet sheet, String label) {
        return ExcelTemplateUtils.findRowIndexByCellContains(sheet, label);
    }

    private Row findRowByLabel(Sheet sheet, String label) {
        int index = findRowIndexByLabel(sheet, label);
        return index >= 0 ? sheet.getRow(index) : null;
    }

    private void fillLabelValue(Sheet sheet, String label, String value) {
        int rowIndex = findRowIndexByLabel(sheet, label);
        if (rowIndex < 0) {
            return;
        }
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return;
        }
        int col = findColumnIndexContains(row, label, 0);
        int targetCol = nextColumnAfterMerged(sheet, rowIndex, col);
        setCellValue(row, targetCol, value, cellFromRow(row, targetCol));
    }

    private int nextColumnAfterMerged(Sheet sheet, int rowIndex, int colIndex) {
        if (rowIndex < 0 || colIndex < 0) {
            return colIndex + 1;
        }
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.isInRange(rowIndex, colIndex)) {
                return region.getLastColumn() + 1;
            }
        }
        return colIndex + 1;
    }

    private Map<String, Integer> buildCodeColumnMap(Row codeRow) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (codeRow == null) {
            return map;
        }
        String[] codes = {"1.1","1.2","1.3","2.1","2.2","2.3","2.4","2.5","2.6","2.7","2.8","3.1","3.2","4.1","4.2","4.3","5.1","5.2","5.3","5.4","6.1","6.2","6.3"};
        for (Cell cell : codeRow) {
            String value = ExcelTemplateUtils.getCellString(cell);
            if (value == null) {
                continue;
            }
            for (String code : codes) {
                if (value.trim().equals(code)) {
                    map.put(code, cell.getColumnIndex());
                    break;
                }
            }
        }
        return map;
    }

    private void adjustVoucherColumnWidth(Sheet sheet, int columnIndex) {
        if (columnIndex < 0) {
            return;
        }
        int currentWidth = sheet.getColumnWidth(columnIndex);
        int minWidth = 14 * 256;
        if (currentWidth < minWidth) {
            sheet.setColumnWidth(columnIndex, minWidth);
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

    private Cell cellFromRow(Row row, int columnIndex) {
        if (row == null || columnIndex < 0) {
            return null;
        }
        return row.getCell(columnIndex);
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
