package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PlanImportResult - Resultado da importação de plano
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanImportResult {

    private UUID cutPlanId;
    private Integer itemsImported;
    private Integer itemsSkipped;
    private List<String> skippedReasons;
    private LocalDateTime importedAt;
}
