package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.entity.QuoteItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Service
public class QuoteCommercialConditionsService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final MathContext CALCULATION_CONTEXT = new MathContext(18, RoundingMode.HALF_UP);
    private final BigDecimal configuredMonthlyRate;

    public QuoteCommercialConditionsService(
            @Value("${quote.payment.credit-monthly-rate:0}") BigDecimal configuredMonthlyRate) {
        this.configuredMonthlyRate = configuredMonthlyRate != null ? configuredMonthlyRate : BigDecimal.ZERO;
    }

    public Calculation calculate(Quote quote) {
        BigDecimal original = grossTotal(quote);
        BigDecimal baselineDiscount = original.multiply(nvl(quote.getDiscountPercent()))
                .divide(HUNDRED, CALCULATION_CONTEXT);
        BigDecimal saleTotal = money(original.subtract(baselineDiscount).max(BigDecimal.ZERO));

        BigDecimal cashDiscount = discountAmount(saleTotal, quote.getCashDiscountType(), quote.getCashDiscountValue());
        BigDecimal pixDiscount = discountAmount(saleTotal, quote.getPixDiscountType(), quote.getPixDiscountValue());
        BigDecimal cashPercent = percentOf(cashDiscount, saleTotal);
        BigDecimal pixPercent = percentOf(pixDiscount, saleTotal);
        int installments = quote.getCreditInstallments();
        boolean interest = quote.getCreditWithInterest() && installments > 0;
        BigDecimal monthlyRate = quote.getCreditMonthlyInterestRate() != null
                ? quote.getCreditMonthlyInterestRate() : configuredMonthlyRate;
        BigDecimal cardTotal = saleTotal;
        if (interest && monthlyRate.signum() > 0) {
            BigDecimal factor = BigDecimal.ONE.add(monthlyRate.divide(HUNDRED, CALCULATION_CONTEXT))
                    .pow(installments, CALCULATION_CONTEXT);
            cardTotal = money(saleTotal.multiply(factor, CALCULATION_CONTEXT));
        }
        BigDecimal installmentAmount = installments > 0
                ? cardTotal.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal lastInstallmentAmount = installments > 0
                ? cardTotal.subtract(installmentAmount.multiply(BigDecimal.valueOf(installments - 1L))).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal boletoTotal = quote.getBoletoValue() != null && quote.getBoletoValue().signum() > 0
                ? money(quote.getBoletoValue()) : saleTotal;

        return new Calculation(original, saleTotal, cashDiscount, cashPercent,
                money(saleTotal.subtract(cashDiscount)), pixDiscount, pixPercent,
                money(saleTotal.subtract(pixDiscount)), monthlyRate, installments,
                interest, cardTotal, installmentAmount, lastInstallmentAmount, quote.getBoletoEnabled(),
                boletoTotal, quote.getBoletoInstallments(), quote.getBoletoFirstDueDate(),
                quote.getBoletoIntervalDays());
    }

    public BigDecimal grossTotal(Quote quote) {
        if (quote == null || quote.getItems() == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = BigDecimal.ZERO;
        for (QuoteItem item : quote.getItems()) {
            if (item != null) total = total.add(item.getSubtotal());
        }
        return total;
    }

    public void applyAndValidate(Quote quote) {
        if (quote.getCashDiscountType() == null || quote.getCashDiscountType().isBlank()) quote.setCashDiscountType("PERCENT");
        if (quote.getPixDiscountType() == null || quote.getPixDiscountType().isBlank()) quote.setPixDiscountType("PERCENT");
        quote.setCashDiscountType(normalizeType(quote.getCashDiscountType()));
        quote.setPixDiscountType(normalizeType(quote.getPixDiscountType()));
        quote.setCashDiscountValue(nvl(quote.getCashDiscountValue()).setScale(6, RoundingMode.HALF_UP));
        quote.setPixDiscountValue(nvl(quote.getPixDiscountValue()).setScale(6, RoundingMode.HALF_UP));
        quote.setCreditInstallments(quote.getCreditInstallments());
        quote.setBoletoInstallments(quote.getBoletoInstallments());
        if (quote.getCreditMonthlyInterestRate() == null) quote.setCreditMonthlyInterestRate(configuredMonthlyRate);
        quote.setCreditMonthlyInterestRate(quote.getCreditMonthlyInterestRate().setScale(4, RoundingMode.HALF_UP));

        validateDiscount(quote.getCashDiscountType(), quote.getCashDiscountValue(), "dinheiro");
        validateDiscount(quote.getPixDiscountType(), quote.getPixDiscountValue(), "PIX");
        BigDecimal legacyDiscountPercent = nvl(quote.getDiscountPercent());
        if (legacyDiscountPercent.signum() < 0 || legacyDiscountPercent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("O desconto geral deve estar entre 0% e 100%.");
        }
        validateInstallments(quote.getCreditInstallments(), "crédito");
        validateInstallments(quote.getBoletoInstallments(), "boleto");
        if (quote.getCreditMonthlyInterestRate().signum() < 0 || quote.getCreditMonthlyInterestRate().compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("A taxa mensal do cartão deve estar entre 0% e 100%.");
        }
        if (quote.getBoletoValue() != null && quote.getBoletoValue().signum() < 0) {
            throw new IllegalArgumentException("O valor do boleto não pode ser negativo.");
        }
        if (quote.getBoletoIntervalDays() != null && quote.getBoletoIntervalDays() < 1) {
            throw new IllegalArgumentException("O intervalo do boleto deve ser de pelo menos 1 dia.");
        }
        Calculation calculation = calculate(quote);
        if (quote.getCashDiscountValue().compareTo(calculation.baseSaleTotal()) > 0 && "AMOUNT".equals(quote.getCashDiscountType())) {
            throw new IllegalArgumentException("O desconto em dinheiro não pode superar o valor do orçamento.");
        }
        if (quote.getPixDiscountValue().compareTo(calculation.baseSaleTotal()) > 0 && "AMOUNT".equals(quote.getPixDiscountType())) {
            throw new IllegalArgumentException("O desconto no PIX não pode superar o valor do orçamento.");
        }
    }

    private BigDecimal discountAmount(BigDecimal base, String type, BigDecimal input) {
        BigDecimal value = nvl(input);
        BigDecimal amount = "AMOUNT".equalsIgnoreCase(type)
                ? value : base.multiply(value).divide(HUNDRED, CALCULATION_CONTEXT);
        return money(amount.min(base).max(BigDecimal.ZERO));
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal base) {
        if (base.signum() == 0) return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        return amount.multiply(HUNDRED).divide(base, 4, RoundingMode.HALF_UP);
    }

    private String normalizeType(String value) {
        String type = value.trim().toUpperCase();
        if (!"PERCENT".equals(type) && !"AMOUNT".equals(type)) {
            throw new IllegalArgumentException("Tipo de desconto inválido. Use percentual ou valor em reais.");
        }
        return type;
    }

    private void validateDiscount(String type, BigDecimal value, String label) {
        if (value.signum() < 0) throw new IllegalArgumentException("O desconto de " + label + " não pode ser negativo.");
        if ("PERCENT".equals(type) && value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("O desconto percentual de " + label + " deve ser de até 100%.");
        }
    }

    private void validateInstallments(Integer value, String label) {
        if (value == null || value < 0 || value > 10) {
            throw new IllegalArgumentException("A quantidade de parcelas de " + label + " deve estar entre 0 e 10.");
        }
    }

    private BigDecimal nvl(BigDecimal value) { return value != null ? value : BigDecimal.ZERO; }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    public record Calculation(BigDecimal originalTotal, BigDecimal baseSaleTotal,
                              BigDecimal cashDiscountAmount, BigDecimal cashDiscountPercent,
                              BigDecimal cashFinalTotal, BigDecimal pixDiscountAmount,
                              BigDecimal pixDiscountPercent, BigDecimal pixFinalTotal,
                              BigDecimal monthlyInterestRate, int creditInstallments,
                              boolean creditWithInterest, BigDecimal creditTotal,
                              BigDecimal creditInstallmentAmount, BigDecimal creditLastInstallmentAmount,
                              boolean boletoEnabled,
                              BigDecimal boletoTotal, int boletoInstallments,
                              java.time.LocalDate boletoFirstDueDate, Integer boletoIntervalDays) { }
}
