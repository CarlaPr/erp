package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * CutPlanUpdateRequest - Requisição para atualizar plano
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanUpdateRequest {

    private String description;
    private String status;
    private List<CutPlanItemRequest> items;
}
