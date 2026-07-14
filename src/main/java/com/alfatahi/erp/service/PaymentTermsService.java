package com.alfatahi.erp.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentTermsService {

    public static final String PLAN_SPLIT_50_50 = "SPLIT_50_50";
    public static final String PLAN_FULL_UPFRONT = "FULL_UPFRONT";
    public static final String PLAN_FULL_ON_DELIVERY = "FULL_ON_DELIVERY";

    public static final String STAGE_ENTRADA = "entrada";
    public static final String STAGE_ENTREGA = "entrega";
    public static final String STAGE_UNICO = "unico";

    /** Uma parcela planejada: valor, vencimento e a etapa que ela representa. */
    public static class PlannedInstallment {
        private final BigDecimal amount;
        private final LocalDate dueDate;
        private final String stage;

        public PlannedInstallment(BigDecimal amount, LocalDate dueDate, String stage) {
            this.amount = amount;
            this.dueDate = dueDate;
            this.stage = stage;
        }

        public BigDecimal getAmount() { return amount; }
        public LocalDate getDueDate() { return dueDate; }
        public String getStage() { return stage; }
    }

    public List<PlannedInstallment> generateInstallments(String paymentMethod, String paymentPlan,
                                                          BigDecimal totalValue, LocalDate today,
                                                          LocalDate deliveryDate) {
        List<PlannedInstallment> plan = new ArrayList<>();
        String method = paymentMethod != null ? paymentMethod : "";

        boolean isCredito = method.contains("Crédito") || method.contains("Credito") || method.contains("Link de Pagamento");
        boolean isDebito = method.contains("Débito") || method.contains("Debito");

        if (isCredito) {
            plan.add(new PlannedInstallment(totalValue, deliveryDate, STAGE_UNICO));
            return plan;
        }

        if (isDebito) {
            plan.add(new PlannedInstallment(totalValue, deliveryDate, STAGE_UNICO));
            return plan;
        }

        String plano = paymentPlan != null ? paymentPlan : PLAN_SPLIT_50_50;

        switch (plano) {
            case PLAN_FULL_UPFRONT:
                plan.add(new PlannedInstallment(totalValue, today, STAGE_UNICO));
                break;
            case PLAN_FULL_ON_DELIVERY:
                plan.add(new PlannedInstallment(totalValue, deliveryDate, STAGE_UNICO));
                break;
            case PLAN_SPLIT_50_50:
            default:
                BigDecimal entrada = totalValue.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal saldo = totalValue.subtract(entrada);
                plan.add(new PlannedInstallment(entrada, today, STAGE_ENTRADA));
                plan.add(new PlannedInstallment(saldo, deliveryDate, STAGE_ENTREGA));
                break;
        }

        return plan;
    }
}
