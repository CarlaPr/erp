package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GlassDrillingResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassDrillingResponse {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal diameter;
    private BigDecimal rebaixDepth;
    private BigDecimal costPerUnit;
    private Integer timePerHole;
    private Boolean active;
}
