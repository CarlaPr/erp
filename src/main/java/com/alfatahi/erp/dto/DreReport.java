package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record DreReport(
        String view,
        List<YearMonth> months,
        List<String> monthLabels,
        boolean showTotal,
        boolean showVariation,
        String focusLabel,
        List<Line> lines,
        Kpis kpis,
        BigDecimal unclassifiedTotal,
        long unclassifiedCount) {

    public enum Kind { ITEM, SUBTOTAL, RESULT, SECTION, MEMO }

    public record Line(
            String key,
            String label,
            String hint,
            Kind kind,
            BigDecimal[] values,
            BigDecimal total,
            BigDecimal[] av,
            BigDecimal avTotal,
            BigDecimal variation,
            boolean alert,
            boolean drillable,
            List<Detail> details) {

        public boolean isHasDetails() { return details != null && !details.isEmpty(); }
    }

    public record Detail(
            String category,
            String subcategory,
            String label,
            BigDecimal[] values,
            BigDecimal total) { }

    public record Kpis(
            BigDecimal grossRevenue,
            BigDecimal netRevenue,
            BigDecimal grossProfit,
            BigDecimal grossMarginPct,
            BigDecimal operatingResult,
            BigDecimal operatingMarginPct,
            BigDecimal netProfit,
            BigDecimal netMarginPct,
            BigDecimal contributionMargin,
            BigDecimal contributionMarginPct,
            BigDecimal fixedCosts,
            BigDecimal breakEvenRevenue,
            BigDecimal losses,
            BigDecimal lossesPct,
            BigDecimal outsideResult,
            BigDecimal afterAllocations) { }
}
