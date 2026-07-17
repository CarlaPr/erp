package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO de relatório de Ordem de Serviço (individual ou como linha de um relatório consolidado).
 * Reúne: item(ns) vendido(s), custo de produção/materiais, receita, custo total e lucro da O.S.
 */
public class WorkOrderReportDto {

    private UUID id;
    private String number;
    private String title;
    private String status;
    private String categoryName;

    private String clientName;
    private String clientDocument;
    private String clientPhone;
    private String clientCity;

    private LocalDateTime createdAt;
    private LocalDate installDate;

    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal area;

    private List<WorkOrderItemReportDto> items = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() {
        if (status == null) return "—";
        return switch (status) {
            case "pending" -> "Pendente";
            case "in_progress", "in-progress" -> "Em Produção";
            case "completed", "done" -> "Concluída";
            case "cancelled", "canceled" -> "Cancelada";
            default -> status;
        };
    }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientDocument() { return clientDocument; }
    public void setClientDocument(String clientDocument) { this.clientDocument = clientDocument; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public String getClientCity() { return clientCity; }
    public void setClientCity(String clientCity) { this.clientCity = clientCity; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }

    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }

    public List<WorkOrderItemReportDto> getItems() { return items; }
    public void setItems(List<WorkOrderItemReportDto> items) { this.items = items; }

    // ── Totais calculados ──────────────────────────────────────────────
    public BigDecimal getTotalRevenue() {
        return items.stream().map(WorkOrderItemReportDto::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalCost() {
        return items.stream().map(WorkOrderItemReportDto::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getProfit() {
        return getTotalRevenue().subtract(getTotalCost());
    }

    public BigDecimal getMarginPercent() {
        BigDecimal revenue = getTotalRevenue();
        if (revenue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getProfit().multiply(new BigDecimal("100")).divide(revenue, 2, RoundingMode.HALF_UP);
    }
}
