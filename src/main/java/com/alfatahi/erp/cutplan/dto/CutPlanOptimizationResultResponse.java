package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanOptimizationResultResponse {

    private Integer totalSheets;
    private BigDecimal totalAreaUsed;
    private BigDecimal totalAreaWasted;
    private BigDecimal utilizationPercent;
    private Integer totalPieces;
    private BigDecimal standardSheetArea;
    private List<CutPlanSheetResponse> sheets;

    public BigDecimal getWastePercent() {
        return new BigDecimal("100").subtract(utilizationPercent);
    }
}
