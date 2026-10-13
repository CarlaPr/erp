package com.alfatahi.erp.dto;

import java.math.BigDecimal;

public record DreEntry(
        String date,
        String description,
        String party,
        String detail,
        String status,
        BigDecimal amount) { }
