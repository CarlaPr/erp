package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CostTableComparisonResponse - Comparação de preços entre fornecedores
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableComparisonResponse {

    private String category;
    private String itemType;
    private List<SupplierPriceComparison> supplierPrices;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
    private BigDecimal averagePrice;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SupplierPriceComparison {
        private String supplierName;
        private BigDecimal price;
        private String unit;
        private LocalDateTime effectiveFrom;
        private LocalDateTime effectiveTo;
        private Boolean isCurrent;

        public String getPriceStatus() {
            return isCurrent ? "VIGENTE" : "EXPIRADO";
        }
    }
}
