package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class WorkOrderReportTotalsDto {

    private int quantity = 0;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalCost = BigDecimal.ZERO;

    public void accumulate(WorkOrderReportDto os) {
        quantity++;
        totalRevenue = totalRevenue.add(os.getTotalRevenue());
        totalCost = totalCost.add(os.getTotalCost());
    }

    public int getQuantity() { return quantity; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getTotalProfit() { return totalRevenue.subtract(totalCost); }

    public BigDecimal getAverageMargin() {
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getTotalProfit().multiply(new BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getAverageTicket() {
        if (quantity == 0) return BigDecimal.ZERO;
        return totalRevenue.divide(new BigDecimal(quantity), 2, RoundingMode.HALF_UP);
    }
}
