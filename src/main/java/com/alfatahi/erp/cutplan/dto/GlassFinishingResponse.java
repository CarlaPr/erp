package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GlassFinishingResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassFinishingResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal costAdjustment;
    private String adjustmentType;
    private Integer processingTimeMinutes;
    private Boolean active;
}
