package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * ExportPlanRequest - Configurações para exportação de plano
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportPlanRequest {

    private UUID cutPlanId;
    private String format;  // PDF, EXCEL, CSV
    private Boolean includeHistoryy;
    private Boolean includeCostDetails;
    private Boolean includeOptimizationLayout;
    private String fileName;
}
