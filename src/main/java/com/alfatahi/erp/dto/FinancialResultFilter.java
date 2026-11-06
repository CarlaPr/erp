package com.alfatahi.erp.dto;

import com.alfatahi.erp.service.FinancialPeriod;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record FinancialResultFilter(LocalDate from, LocalDate to, String category, UUID workOrderId,
                                    String type, String receiptStatus, String paymentStatus, String sort) {
    public FinancialResultFilter {
        if (from == null || to == null || from.isAfter(to) || to.equals(LocalDate.MAX)) {
            throw new IllegalArgumentException("Informe as datas inicial e final em um intervalo válido.");
        }
        category = clean(category).toUpperCase(Locale.ROOT);
        type = clean(type).toUpperCase(Locale.ROOT);
        receiptStatus = clean(receiptStatus);
        paymentStatus = clean(paymentStatus);
        sort = clean(sort).isEmpty() ? "date" : sort;
        if (!Set.of("", "ENTRADA", "SAIDA").contains(type)
                || !Set.of("", "pending", "partial", "received").contains(receiptStatus)
                || !Set.of("", "pending", "partial", "paid").contains(paymentStatus)
                || !Set.of("date", "date_desc", "amount_desc", "description").contains(sort)) {
            throw new IllegalArgumentException("Filtro financeiro inválido.");
        }
    }

    public static FinancialResultFilter select(String month, LocalDate from, LocalDate to, String category,
                                               UUID order, String type, String receipt, String payment, String sort) {
        if (from == null && to == null) {
            FinancialPeriod period = month == null || month.isBlank() ? FinancialPeriod.current()
                    : FinancialPeriod.monthly(YearMonth.parse(month));
            from = period.from();
            to = period.to();
        }
        return new FinancialResultFilter(from, to, category, order, type, receipt, payment, sort);
    }

    public LocalDate endExclusive() { return to.plusDays(1); }
    public boolean contains(LocalDate date) { return date != null && !date.isBefore(from) && !date.isAfter(to); }
    public boolean filtered() {
        return !category.isEmpty() || workOrderId != null || !type.isEmpty() || !receiptStatus.isEmpty() || !paymentStatus.isEmpty();
    }
    public FinancialResultFilter previous() {
        if (from.getDayOfMonth() == 1 && to.equals(from.plusMonths(1).minusDays(1))) {
            return new FinancialResultFilter(from.minusMonths(1), from.minusDays(1), category, workOrderId, type, receiptStatus, paymentStatus, sort);
        }
        if (from.getDayOfMonth() == 6 && to.equals(from.plusMonths(1).minusDays(1))) {
            return new FinancialResultFilter(from.minusMonths(1), from.minusDays(1), category, workOrderId, type, receiptStatus, paymentStatus, sort);
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return new FinancialResultFilter(from.minusDays(days), from.minusDays(1), category, workOrderId, type, receiptStatus, paymentStatus, sort);
    }
    private static String clean(String value) { return value == null ? "" : value.strip(); }
}
