package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuotePaymentSummary {

    private QuotePaymentSummary() { }

    public record PaymentLine(String method, String amount) {
        public String getMethod() { return method; }
        public String getAmount() { return amount; }
    }
    public record Summary(List<PaymentLine> lines, List<PaymentLine> pdfLines, String installmentsText) { }

    public static Summary from(Quote quote, BigDecimal total) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        if (quote.getPaymentBreakdown() != null) values.putAll(quote.getPaymentBreakdown());

        List<String> methods = selectedMethods(quote.getPaymentMethod());
        boolean splitEnabled = methods.size() > 1 && values.size() == methods.size()
                && methods.stream().allMatch(method -> values.get(method) != null && values.get(method).signum() > 0);

        List<PaymentLine> lines = new ArrayList<>();
        List<PaymentLine> pdfLines = new ArrayList<>();
        List<PaymentLine> cashPixLines = new ArrayList<>();
        List<PaymentLine> otherLines = new ArrayList<>();
        BigDecimal cardTotal = BigDecimal.ZERO;
        List<String> cardMethods = new ArrayList<>();
        if (splitEnabled) {
            for (String method : methods) {
                BigDecimal amount = values.get(method);
                lines.add(new PaymentLine(method, formatBRL(amount)));
                if (isCashOrPix(method)) {
                    cashPixLines.add(new PaymentLine("Valor via " + method, formatBRL(amount)));
                } else if (isCreditMethod(method)) {
                    cardMethods.add(method);
                    cardTotal = cardTotal.add(amount);
                } else {
                    otherLines.add(new PaymentLine(method, formatBRL(amount)));
                }
            }
            pdfLines.addAll(cashPixLines);
            pdfLines.addAll(otherLines);
            if (cardTotal.signum() > 0) {
                pdfLines.add(new PaymentLine(cardLabel(cardMethods, quote.getInstallments()), formatBRL(cardTotal)));
            }
        } else if (methods.stream().anyMatch(QuotePaymentSummary::isCreditMethod)) {
            cardTotal = total != null ? total : BigDecimal.ZERO;
        }

        String installmentsText = "";
        if (cardTotal.signum() > 0) {
            int count = quote.getInstallments() != null && quote.getInstallments() > 0 ? quote.getInstallments() : 1;
            BigDecimal installmentValue = cardTotal.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            installmentsText = "Em " + count + (count == 1 ? " vez" : " vezes")
                    + " de " + formatBRL(installmentValue);
        }
        return new Summary(lines, pdfLines, installmentsText);
    }

    private static List<String> selectedMethods(String methods) {
        if (methods == null || methods.isBlank()) return List.of();
        return java.util.Arrays.stream(methods.split(","))
                .map(String::trim)
                .filter(method -> !method.isEmpty() && !"Não Informado".equalsIgnoreCase(method))
                .toList();
    }

    private static boolean isCreditMethod(String method) {
        String normalized = method == null ? "" : method.trim().toLowerCase(Locale.ROOT);
        return "crédito".equals(normalized) || "credito".equals(normalized) || "link de pagamento".equals(normalized);
    }

    private static boolean isCashOrPix(String method) {
        String normalized = method == null ? "" : method.trim().toLowerCase(Locale.ROOT);
        return "dinheiro".equals(normalized) || "pix".equals(normalized);
    }

    private static String cardLabel(List<String> methods, Integer installments) {
        boolean split = installments != null && installments > 1;
        if (methods.size() == 1 && "link de pagamento".equalsIgnoreCase(methods.get(0))) {
            return split ? "Valor Parcelado no Link de Pagamento" : "Valor no Link de Pagamento";
        }
        if (methods.size() == 1) return split ? "Valor Parcelado no Cartão de Crédito" : "Valor no Cartão de Crédito";
        return split ? "Valor Parcelado no Cartão / Link" : "Valor no Cartão / Link";
    }

    private static String formatBRL(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return formatter.format(value);
    }
}
