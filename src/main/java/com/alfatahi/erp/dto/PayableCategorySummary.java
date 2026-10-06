package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.util.List;

public record PayableCategorySummary(List<Category> categories, List<Category> subcategories, long count,
                                     BigDecimal totalAmount, BigDecimal paidAmount) {
    public record Category(String code, String name, boolean unconfigured, long count,
                           BigDecimal totalAmount, BigDecimal paidAmount,
                           BigDecimal countPercentage, BigDecimal totalPercentage, BigDecimal paidPercentage) {
    }
}
