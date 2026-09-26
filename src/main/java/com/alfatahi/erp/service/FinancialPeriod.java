package com.alfatahi.erp.service;

import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Competência financeira: do dia 6 ao dia 5 do mês seguinte, inclusive. */
public record FinancialPeriod(YearMonth reference, LocalDate from, LocalDate to) {
    public static final int START_DAY = 6;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"));
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    public static YearMonth referenceFor(LocalDate date) {
        YearMonth month = YearMonth.from(date);
        return date.getDayOfMonth() < START_DAY ? month.minusMonths(1) : month;
    }

    public static FinancialPeriod monthly(YearMonth reference) {
        LocalDate start = reference.atDay(START_DAY);
        return new FinancialPeriod(reference, start, start.plusMonths(1).minusDays(1));
    }

    public static FinancialPeriod current() {
        return monthly(referenceFor(today()));
    }

    public static FinancialPeriod select(String month, boolean allMonths, LocalDate from, LocalDate to) {
        if (allMonths || "all".equalsIgnoreCase(month)) return new FinancialPeriod(null, null, null);
        if (from != null || to != null) return new FinancialPeriod(null, from, to);
        if (month != null && !month.isBlank()) {
            try {
                return monthly(YearMonth.parse(month));
            } catch (DateTimeParseException ignored) {
                // Filtro inválido volta ao período atual, sem ampliar a consulta para todo o histórico.
            }
        }
        return current();
    }

    public LocalDate endExclusive() {
        return to == null ? null : to.plusDays(1);
    }

    public boolean contains(LocalDate date) {
        if (from == null && to == null) return true;
        return date != null && (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    public boolean contains(LocalDateTime date) {
        return contains(date == null ? null : date.toLocalDate());
    }

    public String monthValue() {
        return reference == null ? "" : reference.toString();
    }

    public String label() {
        if (reference != null) return reference.format(MONTH_LABEL);
        if (from == null && to == null) return "Todos os meses";
        if (from == null) return "Até " + to.format(DATE_LABEL);
        if (to == null) return "A partir de " + from.format(DATE_LABEL);
        return from.format(DATE_LABEL) + " a " + to.format(DATE_LABEL);
    }

    public void addTo(Model model) {
        model.addAttribute("reportMonth", monthValue());
        model.addAttribute("reportLabel", label());
        model.addAttribute("reportFrom", from);
        model.addAttribute("reportTo", to);
        model.addAttribute("reportAllMonths", from == null && to == null);
    }
}
