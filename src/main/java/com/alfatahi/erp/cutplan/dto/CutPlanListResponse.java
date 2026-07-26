package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanListResponse {
    private UUID id;
    private UUID workOrderId;
    private String workOrderNumber;
    private String status;
    private Integer version;
    private Integer totalItems;
    private Integer itemCount;
    private BigDecimal totalEstimatedCost;
    private LocalDateTime createdAt;
    private String createdBy;
}
