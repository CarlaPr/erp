package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BulkPriceUpdateResult - Resultado da atualização em lote
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkPriceUpdateResult {

    private Integer totalUpdated;
    private BigDecimal totalValueChange;
    private LocalDateTime executedAt;
    private String executedBy;
    private List<CostTableResponse> updatedPrices;
}
