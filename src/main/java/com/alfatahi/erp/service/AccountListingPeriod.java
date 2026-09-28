package com.alfatahi.erp.service;

import org.springframework.ui.Model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record AccountListingPeriod(FinancialPeriod period, boolean currentWeek) {
    public static AccountListingPeriod select(String month, boolean allMonths,
                                              LocalDate from, LocalDate to, boolean currentWeek) {
        return select(month, allMonths, from, to, currentWeek, FinancialPeriod.today());
    }

    static AccountListingPeriod select(String month, boolean allMonths,
                                       LocalDate from, LocalDate to, boolean currentWeek, LocalDate today) {
        if (allMonths || "all".equalsIgnoreCase(month)) {
            return new AccountListingPeriod(FinancialPeriod.select(month, true, from, to), false);
        }
        if (currentWeek || ((month == null || month.isBlank()) && from == null && to == null)) {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new AccountListingPeriod(new FinancialPeriod(null, monday, monday.plusDays(6)), true);
        }
        return new AccountListingPeriod(FinancialPeriod.select(month, false, from, to), false);
    }

    public boolean contains(LocalDate dueDate, String status) {
        return period.contains(dueDate)
                || (currentWeek && dueDate != null && dueDate.isBefore(period.from())
                && ("pending".equals(status) || "partial".equals(status)));
    }

    public void addTo(Model model) {
        period.addTo(model);
        model.addAttribute("filterCurrentWeek", currentWeek);
    }
}
