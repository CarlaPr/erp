package com.alfatahi.erp.dto;

import java.math.BigDecimal;

public record PayableCategoryTotals(String category, String subcategory, long count, BigDecimal totalAmount, BigDecimal paidAmount) {
}
