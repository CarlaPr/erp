package com.alfatahi.erp.dto;

import java.util.List;

/** Representa uma sub-tabela extra dentro de um relatório PDF genérico (financial-report-pdf.html). */
public class ReportSectionDto {

    private String title;
    private List<String> columns;
    private List<List<String>> rows;
    private List<String> totalsRow;

    public ReportSectionDto(String title, List<String> columns, List<List<String>> rows, List<String> totalsRow) {
        this.title = title;
        this.columns = columns;
        this.rows = rows;
        this.totalsRow = totalsRow;
    }

    public String getTitle() { return title; }
    public List<String> getColumns() { return columns; }
    public List<List<String>> getRows() { return rows; }
    public List<String> getTotalsRow() { return totalsRow; }
}
