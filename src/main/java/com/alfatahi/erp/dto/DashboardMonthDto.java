package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardMonthDto(int month, String label, LocalDate from, LocalDate to,
                                BigDecimal revenue, BigDecimal income, BigDecimal expenses,
                                BigDecimal closingBalance) {
    public BigDecimal getProfit() {
        return income.subtract(expenses);
    }
}
