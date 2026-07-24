package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CostTableCreateRequest - Criar nova entrada de preço
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableCreateRequest {

    private String category;
    private String itemType;
    private String description;
    private BigDecimal unitPrice;
    private String unit;
    private UUID supplierId;
    private String effectiveFrom;
    private String effectiveTo;
    private String remarks;
}
