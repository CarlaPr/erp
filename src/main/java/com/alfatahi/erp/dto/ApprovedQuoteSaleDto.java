package com.alfatahi.erp.dto;

import com.alfatahi.erp.entity.QuoteItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ApprovedQuoteSaleDto(UUID quoteId, String quoteNumber, String workOrderNumber, String clientName,
                                   LocalDate approvedDate, BigDecimal saleTotal, List<QuoteItem> items) { }
