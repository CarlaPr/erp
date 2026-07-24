package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WorkOrderToCutPlanConversionResult - Resultado da conversão de OS para Plano
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrderToCutPlanConversionResult {

    private UUID cutPlanId;
    private UUID workOrderId;
    private String workOrderNumber;
    private Integer itemsCreated;
    private Integer itemsWithRulesApplied;
    private Integer itemsWithCostsCalculated;
    private BigDecimal totalEstimatedCost;
    private LocalDateTime createdAt;
    private String createdBy;
}
