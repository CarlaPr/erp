package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.util.List;

public record ServiceSalesSummaryDto(long totalItems, BigDecimal totalAmount, List<ServiceSalesGroupDto> groups) {
    public boolean isEmpty() {
        return groups == null || groups.isEmpty();
    }
}
