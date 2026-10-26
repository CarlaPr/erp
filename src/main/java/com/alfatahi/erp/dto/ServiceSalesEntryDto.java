package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ServiceSalesEntryDto(String clientName, String workOrderNumber, String quoteNumber,
                                   LocalDate approvedDate, long items, String unitsLabel, boolean showUnits,
                                   BigDecimal amount) { }
