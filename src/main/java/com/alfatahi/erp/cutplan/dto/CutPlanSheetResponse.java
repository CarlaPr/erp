package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * CutPlanSheetResponse - Informações sobre uma chapa otimizada
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanSheetResponse {

    private String sheetId;
    private Integer piecesCount;
    private BigDecimal areaUsed;
    private BigDecimal areaWasted;
    private BigDecimal utilizationPercent;
    private List<String> pieceDescriptions;
}
