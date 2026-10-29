package com.alfatahi.erp.exception;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AgendaDayBlockedException extends RuntimeException {
    private final LocalDate date;
    private final String reason;

    public AgendaDayBlockedException(LocalDate date, String reason) {
        super("A agenda de " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " está bloqueada para novos serviços e visitas técnicas.");
        this.date = date;
        this.reason = reason;
    }

    public LocalDate getDate() { return date; }
    public String getReason() { return reason; }
}
