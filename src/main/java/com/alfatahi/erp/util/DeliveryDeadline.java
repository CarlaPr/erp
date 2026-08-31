package com.alfatahi.erp.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DeliveryDeadline {
    private DeliveryDeadline() {
    }


    public static LocalDate fromApproval(LocalDateTime approvalDate) {
        LocalDate date = approvalDate.toLocalDate();
        int remaining = 15;
        while (remaining > 0) {
            date = date.plusDays(1);
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
                remaining--;
            }
        }
        return date;
    }
}
