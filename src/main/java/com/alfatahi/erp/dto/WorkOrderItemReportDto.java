package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Representa uma linha (item/material/serviço) dentro do relatório de uma Ordem de Serviço,
 * já com o custo de produção/material, o preço de venda e a margem calculados.
 */
public class WorkOrderItemReportDto {

    private String description;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private BigDecimal unitPrice = BigDecimal.ZERO;

    public WorkOrderItemReportDto() {}

    public WorkOrderItemReportDto(String description, BigDecimal quantity, BigDecimal unitCost, BigDecimal unitPrice) {
        this.description = description;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTotalCost() { return quantity.multiply(unitCost); }

    public BigDecimal getTotalPrice() { return quantity.multiply(unitPrice); }

    public BigDecimal getProfit() { return getTotalPrice().subtract(getTotalCost()); }

    public BigDecimal getMarginPercent() {
        if (getTotalPrice().compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getProfit().multiply(new BigDecimal("100")).divide(getTotalPrice(), 2, RoundingMode.HALF_UP);
    }
}
