package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CutPlanResponse - Resposta com dados completos do plano
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanResponse {

    private UUID id;
    private UUID workOrderId;
    private Integer version;
    private String status;
    private String description;
    private List<CutPlanItemResponse> items;
    private BigDecimal totalEstimatedCost;
    private Integer totalQuantity;
    private BigDecimal totalArea;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
