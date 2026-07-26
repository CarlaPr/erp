package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CutPlanItemRequest - Requisição para criar/atualizar item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanItemRequest {

    private String description;
    private String environment;
    private String glassType;
    private BigDecimal thickness;
    private String color;
    private String finishing;
    private BigDecimal grossWidth;
    private BigDecimal grossHeight;
    private BigDecimal finalWidth;
    private BigDecimal finalHeight;
    private Integer quantity;
    private UUID supplierId;
    private String notes;
    private BigDecimal angle;
    private BigDecimal drillingDiameter;
    private Integer drillingQuantity;
    private BigDecimal drillingCostPerUnit;
    private String notchDescription;
    private BigDecimal notchCost;
}
