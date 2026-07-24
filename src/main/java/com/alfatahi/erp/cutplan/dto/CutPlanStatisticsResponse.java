package com.alfatahi.erp.cutplan.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanStatisticsResponse {
    private UUID planId;
    private UUID cutPlanId;
    private Integer totalItems;
    private Integer totalQuantity;
    private BigDecimal totalArea;
    private BigDecimal totalAreaInSquareMeters;
    private BigDecimal totalWeight;
    private BigDecimal totalEstimatedCost;
    private BigDecimal averageCostPerItem;
    private Long itemsWithDrillings;
    private Long itemsWithNotches;
    private String status;
}
