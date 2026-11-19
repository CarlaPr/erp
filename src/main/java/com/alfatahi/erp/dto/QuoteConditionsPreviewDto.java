package com.alfatahi.erp.dto;

import com.alfatahi.erp.service.QuoteCommercialConditionsService;

import java.util.List;
import java.util.Map;

public record QuoteConditionsPreviewDto(
        QuoteCommercialConditionsService.Calculation calculation,
        List<Map<String, String>> conditionRows,
        List<Map<String, String>> agreedPaymentRows) { }
