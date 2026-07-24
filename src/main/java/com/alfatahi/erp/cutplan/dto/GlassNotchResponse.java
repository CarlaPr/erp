package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GlassNotchResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassNotchResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal width;
    private BigDecimal depth;
    private String shape;
    private BigDecimal cost;
    private Integer processingTime;
    private BigDecimal minLength;
    private BigDecimal maxLength;
    private Boolean active;
}
