package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.entity.CutPlanItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;



/**
 * Resultado de otimização por tipo de vidro
 */
@Data
@Builder
class CutPlanOptimizationResultByGlassType {
    private Map<String, CutPlanOptimizationResult> resultsByType;
    private int totalSheets;
    private BigDecimal totalAreaUsed;
    private BigDecimal totalAreaWasted;
    private BigDecimal overallUtilizationPercent;
}
