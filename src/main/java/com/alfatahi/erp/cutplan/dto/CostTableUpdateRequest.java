package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * CostTableUpdateRequest - Atualizar preço
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableUpdateRequest {

    private BigDecimal unitPrice;
    private String effectiveTo;
    private String reason;
    private String reference;
}
