package com.vayen.rdcm.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ExcelTemplateUtils {

    private ExcelTemplateUtils() {
    }

    public static Workbook loadTemplate(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("模板路径不能为空");
        }
        Path resolvedPath = Path.of(path);
        if (!Files.exists(resolvedPath)) {
            throw new IllegalArgumentException("模板文件不存在: " + path);
        }
        try (InputStream inputStream = Files.newInputStream(resolvedPath)) {
            return WorkbookFactory.create(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("读取模板文件失败: " + path, ex);
        }
    }

    public static Sheet findSheetByNameContains(Workbook workbook, String keyword) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (sheet.getSheetName() != null && sheet.getSheetName().contains(keyword)) {
                return sheet;
            }
        }
        return null;
    }

    public static Sheet cloneSheet(Workbook workbook, Sheet templateSheet, String newName) {
        if (templateSheet == null) {
            throw new IllegalArgumentException("模板工作表不存在");
        }
        int index = workbook.getSheetIndex(templateSheet);
        Sheet cloned = workbook.cloneSheet(index);
        int clonedIndex = workbook.getSheetIndex(cloned);
        workbook.setSheetName(clonedIndex, newName);
        return cloned;
    }

    public static int findRowIndexByCellValue(Sheet sheet, String text) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                String value = getCellString(cell);
                if (text.equals(value)) {
                    return row.getRowNum();
                }
            }
        }
        return -1;
    }

    public static int findRowIndexByCellContains(Sheet sheet, String text) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                String value = getCellString(cell);
                if (value != null && value.contains(text)) {
                    return row.getRowNum();
                }
            }
        }
        return -1;
    }

    public static int findColumnIndex(Row row, String text) {
        if (row == null) {
            return -1;
        }
        for (Cell cell : row) {
            String value = getCellString(cell);
            if (text.equals(value)) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    public static String getCellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (type == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue()).trim();
        }
        if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue()).trim();
        }
        if (type == CellType.FORMULA) {
            return Objects.toString(cell.getCellFormula(), "").trim();
        }
        return null;
    }
}
