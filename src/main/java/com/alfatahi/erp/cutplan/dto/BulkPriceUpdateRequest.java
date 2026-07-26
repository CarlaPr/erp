package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BulkPriceUpdateRequest - Requisição para atualizar múltiplos preços
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPriceUpdateRequest {

    private String category;
    private BigDecimal priceMultiplier;  // Ex: 1.10 = +10%
    private String reason;
    private String effectiveFromDate;
}
