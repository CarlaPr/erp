package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CutPlanCostBreakdown - Breakdown detalhado de custos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanCostBreakdown {

    private String itemDescription;
    private BigDecimal totalCost;
    private BigDecimal glassPercentage;
    private BigDecimal hardwarePercentage;
    private BigDecimal aluminumPercentage;
    private BigDecimal siliconePercentage;

    public String getCostBreakdownString() {
        return String.format(
                "Vidro: %.1f%% | Ferragem: %.1f%% | Alumínio: %.1f%% | Silicone: %.1f%%",
                glassPercentage, hardwarePercentage, aluminumPercentage, siliconePercentage
        );
    }
}