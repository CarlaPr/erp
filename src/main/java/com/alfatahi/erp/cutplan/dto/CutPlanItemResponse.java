package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CutPlanItemResponse - Resposta com dados do item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanItemResponse {

    private UUID id;
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
    private BigDecimal calculatedArea;
    private BigDecimal estimatedWeight;
    private BigDecimal glassCost;
    private BigDecimal hardwaresTotalCost;
    private BigDecimal aluminumTotalCost;
    private BigDecimal siliconeTotalCost;
    private BigDecimal estimatedCost;
    private UUID supplierId;
    private String supplierName;
    private String notes;
    private Boolean sentToSupplier;
    private String supplierFeedback;
    private String shortDescription;
    private BigDecimal areaInSquareMeters;
}
