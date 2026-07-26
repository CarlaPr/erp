package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GlassCutRuleResponse - Resposta com dados da regra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassCutRuleResponse {

    private UUID id;
    private String serviceCategoryName;
    private String ruleType;
    private String ruleTypeLabel;
    private String parameterName;
    private String parameterNameLabel;
    private BigDecimal value;
    private String unit;
    private String unitLabel;
    private String description;
    private String formattedDescription;
    private Boolean active;
    private Integer applicationOrder;
}
