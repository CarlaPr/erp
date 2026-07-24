package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CostTableHistoryResponse - Histórico de mudanças de preço
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableHistoryResponse {

    private UUID id;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private BigDecimal absoluteDifference;
    private BigDecimal percentageDifference;
    private Boolean increase;
    private Boolean decrease;
    private String formattedChange;
    private String changeDescription;
    private String reason;
    private String reference;
    private LocalDateTime changedAt;
    private String changedBy;
}
