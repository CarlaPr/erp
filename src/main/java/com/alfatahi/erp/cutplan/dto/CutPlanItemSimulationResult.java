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
public class CutPlanItemSimulationResult {

    private BigDecimal originalWidth;
    private BigDecimal originalHeight;
    private BigDecimal finalWidth;
    private BigDecimal finalHeight;
    private BigDecimal widthReduction;
    private BigDecimal heightReduction;
    private BigDecimal areaReduction;

    public String getSummary() {
        return String.format(
                "Original: %.0f×%.0f mm → Final: %.0f×%.0f mm | " +
                        "Redução: %.0f×%.0f mm (Área: %.0f mm²)",
                originalWidth, originalHeight, finalWidth, finalHeight,
                widthReduction, heightReduction, areaReduction
        );
    }
}
