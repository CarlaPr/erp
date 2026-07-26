package com.alfatahi.erp.cutplan.dto;

import lombok.*;

import java.util.*;

/**
 * ============================================================================
 * CUT PLAN DTOS - Transfer Objects para API
 * ============================================================================
 */

/**
 * CutPlanCreateRequest - Requisição para criar novo plano de corte
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanCreateRequest {

    private UUID workOrderId;
    private String description;
    private Integer version;
    private String status;
}


