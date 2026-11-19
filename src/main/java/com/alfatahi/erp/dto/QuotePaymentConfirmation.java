package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.util.Map;

public record QuotePaymentConfirmation(Map<String, BigDecimal> payments, Integer creditInstallments) { }
