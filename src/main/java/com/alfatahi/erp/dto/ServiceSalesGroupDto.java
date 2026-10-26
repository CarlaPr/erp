package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.util.List;

public record ServiceSalesGroupDto(String name, long items, String unitsLabel, boolean showUnits,
                                   BigDecimal amount, BigDecimal percentage, boolean showTitles,
                                   List<ServiceSalesDetailDto> details) { }
