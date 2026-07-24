package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanItemCostSimulation {

    private String itemDescription;
    private BigDecimal glassCost;
    private BigDecimal hardwareCost;
    private BigDecimal aluminumCost;
    private BigDecimal siliconeCost;
    private BigDecimal drillingCost;
    private BigDecimal notchCost;
    private BigDecimal totalCost;

    // Percentuais
    public BigDecimal getGlassPercentage() {
        return calculatePercentage(glassCost);
    }

    public BigDecimal getHardwarePercentage() {
        return calculatePercentage(hardwareCost);
    }

    public BigDecimal getAluminumPercentage() {
        return calculatePercentage(aluminumCost);
    }

    public BigDecimal getSiliconePercentage() {
        return calculatePercentage(siliconeCost);
    }

    private BigDecimal calculatePercentage(BigDecimal value) {
        if (totalCost.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        return value.divide(totalCost, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}