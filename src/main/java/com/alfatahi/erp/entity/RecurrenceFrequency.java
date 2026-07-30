package com.alfatahi.erp.entity;

import java.time.LocalDate;

/**
 * Tipos de recorrência suportados para Contas a Pagar (Contas Recorrentes).
 */
public enum RecurrenceFrequency {

    DAILY("Diária"),
    WEEKLY("Semanal"),
    BIWEEKLY("Quinzenal"),
    MONTHLY("Mensal"),
    BIMONTHLY("Bimestral"),
    QUARTERLY("Trimestral"),
    SEMIANNUAL("Semestral"),
    ANNUAL("Anual");

    private final String label;

    RecurrenceFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Calcula a próxima data a partir de uma data base, de acordo com a frequência. */
    public LocalDate next(LocalDate from) {
        return switch (this) {
            case DAILY -> from.plusDays(1);
            case WEEKLY -> from.plusWeeks(1);
            case BIWEEKLY -> from.plusWeeks(2);
            case MONTHLY -> from.plusMonths(1);
            case BIMONTHLY -> from.plusMonths(2);
            case QUARTERLY -> from.plusMonths(3);
            case SEMIANNUAL -> from.plusMonths(6);
            case ANNUAL -> from.plusYears(1);
        };
    }
}
