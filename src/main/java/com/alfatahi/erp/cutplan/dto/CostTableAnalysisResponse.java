package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableAnalysisResponse {

    private String category;
    private String itemType;
    private BigDecimal currentPrice;
    private BigDecimal previousPrice;
    private BigDecimal absoluteChange;
    private BigDecimal percentageChange;
    private LocalDateTime lastChangeDate;
    private String lastChangeReason;

    // Histórico
    private Integer changeCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal averagePrice;

    public String getTrend() {
        if (percentageChange.signum() > 0) {
            return "AUMENTANDO ↑";
        } else if (percentageChange.signum() < 0) {
            return "REDUZINDO ↓";
        } else {
            return "ESTÁVEL →";
        }
    }
}