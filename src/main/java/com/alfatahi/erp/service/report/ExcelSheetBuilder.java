package com.alfatahi.erp.service.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelSheetBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Workbook workbook;
    private final Sheet sheet;
    private int rowIndex = 0;
    private int columnCount = 0;

    private final CellStyle titleStyle;
    private final CellStyle subtitleStyle;
    private final CellStyle headerStyle;
    private final CellStyle currencyStyle;
    private final CellStyle percentStyle;
    private final CellStyle dateStyle;
    private final CellStyle totalsLabelStyle;
    private final CellStyle totalsCurrencyStyle;
    private final CellStyle defaultStyle;

    public ExcelSheetBuilder(String sheetName) {
        this.workbook = new XSSFWorkbook();
        this.sheet = workbook.createSheet(safeSheetName(sheetName));

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);

        Font subtitleFont = workbook.createFont();
        subtitleFont.setItalic(true);
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFont(subtitleFont);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        DataFormat fmt = workbook.createDataFormat();

        currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(fmt.getFormat("R$ #,##0.00"));

        percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(fmt.getFormat("0.00\"%\""));

        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(fmt.getFormat("dd/mm/yyyy"));

        Font totalsFont = workbook.createFont();
        totalsFont.setBold(true);
        totalsLabelStyle = workbook.createCellStyle();
        totalsLabelStyle.setFont(totalsFont);
        totalsLabelStyle.setBorderTop(BorderStyle.MEDIUM);

        totalsCurrencyStyle = workbook.createCellStyle();
        totalsCurrencyStyle.setFont(totalsFont);
        totalsCurrencyStyle.setDataFormat(fmt.getFormat("R$ #,##0.00"));
        totalsCurrencyStyle.setBorderTop(BorderStyle.MEDIUM);

        defaultStyle = workbook.createCellStyle();
    }

    private static String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/\\?\\*\\[\\]:]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    public ExcelSheetBuilder title(String title, String subtitle) {
        Row r1 = sheet.createRow(rowIndex++);
        Cell c1 = r1.createCell(0);
        c1.setCellValue(title);
        c1.setCellStyle(titleStyle);

        if (subtitle != null && !subtitle.isBlank()) {
            Row r2 = sheet.createRow(rowIndex++);
            Cell c2 = r2.createCell(0);
            c2.setCellValue(subtitle);
            c2.setCellStyle(subtitleStyle);
        }
        rowIndex++; // linha em branco
        return this;
    }

    public ExcelSheetBuilder header(String... columns) {
        columnCount = columns.length;
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
        return this;
    }

    /** Adiciona uma linha de dados. Aceita String, BigDecimal/Number, LocalDate, LocalDateTime, PercentValue. */
    public ExcelSheetBuilder row(Object... values) {
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < values.length; i++) {
            writeCell(row.createCell(i), values[i], currencyStyle, dateStyle, percentStyle, defaultStyle);
        }
        return this;
    }

    public ExcelSheetBuilder totalsRow(Object... values) {
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            writeCell(cell, values[i], totalsCurrencyStyle, dateStyle, percentStyle, totalsLabelStyle);
        }
        return this;
    }

    public ExcelSheetBuilder blankRow() {
        rowIndex++;
        return this;
    }

    private void writeCell(Cell cell, Object value, CellStyle currency, CellStyle date, CellStyle percent, CellStyle text) {
        if (value == null) {
            cell.setCellValue("");
            cell.setCellStyle(text);
        } else if (value instanceof PercentValue pv) {
            cell.setCellValue(pv.value().doubleValue());
            cell.setCellStyle(percent);
        } else if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
            cell.setCellStyle(currency);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            cell.setCellStyle(text);
        } else if (value instanceof LocalDate ld) {
            cell.setCellValue(ld.format(DATE_FMT));
            cell.setCellStyle(text);
        } else if (value instanceof LocalDateTime ldt) {
            cell.setCellValue(ldt.format(DATETIME_FMT));
            cell.setCellStyle(text);
        } else {
            cell.setCellValue(String.valueOf(value));
            cell.setCellStyle(text);
        }
    }

    /** Marca um valor numérico para ser exibido como percentual (ex: 12.34 -> "12,34%"). */
    public record PercentValue(BigDecimal value) {
        public static PercentValue of(BigDecimal v) { return new PercentValue(v == null ? BigDecimal.ZERO : v); }
    }

    public byte[] build() {
        for (int i = 0; i < Math.max(columnCount, 1); i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(width + 512, 3000), 14000));
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            workbook.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar arquivo Excel", e);
        }
    }

    /** Permite adicionar uma nova aba (sheet) dentro do mesmo workbook. */
    public Workbook getWorkbook() { return workbook; }
}
