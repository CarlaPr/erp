package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GlassCutRuleCreateRequest - Criar nova regra técnica
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassCutRuleCreateRequest {

    private UUID serviceCategoryId;
    private String ruleType;
    private String parameterName;
    private BigDecimal value;
    private String unit;
    private String description;
    private Integer applicationOrder;
}
