package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CostTableResponse - Resposta com dados de preço
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableResponse {

    private UUID id;
    private String category;
    private String categoryLabel;
    private String itemType;
    private String description;
    private BigDecimal unitPrice;
    private String unit;
    private String unitLabel;
    private String supplierName;
    private UUID supplierId;
    private String effectiveFrom;
    private String effectiveTo;
    private Boolean active;
    private Boolean current;
    private Boolean expired;
    private String formattedDescription;
    private String remarks;
}
