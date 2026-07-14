package com.alfatahi.erp.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Centraliza as regras reais de forma de pagamento da vidraçaria:
 *
 * PIX      -> normalmente 50% entrada / 50% entrega, mas pode ser 100% antecipado.
 * Dinheiro -> 100% antecipado, 50/50, ou 100% na entrega.
 * Débito   -> sempre integral, dinheiro entra praticamente no mesmo dia.
 * Crédito  -> sempre 1 parcela única e líquida: a operadora é quem parcela para o
 *             cliente, a empresa recebe um único depósito. NUNCA gerar N contas a receber
 *             pelo número de parcelas do cartão do cliente.
 *
 * Reaproveitada tanto na aprovação de Orçamento quanto na criação manual de
 * Contas a Receber para uma Ordem de Serviço, para que a regra nunca fique duplicada
 * ou divergente entre os dois pontos de entrada.
 */
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

    /**
     * Gera o plano de parcelas de acordo com a forma de pagamento.
     *
     * @param paymentMethod texto livre vindo do formulário (ex.: "PIX", "Dinheiro", "Débito",
     *                      "Crédito"); a checagem é por conteúdo (contains) para tolerar
     *                      combinações como "PIX, Débito" escolhidas no orçamento.
     * @param paymentPlan   SPLIT_50_50 / FULL_UPFRONT / FULL_ON_DELIVERY — só é considerado
     *                      para PIX e Dinheiro.
     * @param totalValue    valor cheio (bruto) da venda.
     * @param today         data de hoje / aprovação (usada como vencimento da entrada).
     * @param deliveryDate  data prevista de entrega/instalação.
     */
    public List<PlannedInstallment> generateInstallments(String paymentMethod, String paymentPlan,
                                                          BigDecimal totalValue, LocalDate today,
                                                          LocalDate deliveryDate) {
        List<PlannedInstallment> plan = new ArrayList<>();
        String method = paymentMethod != null ? paymentMethod : "";

        boolean isCredito = method.contains("Crédito") || method.contains("Credito") || method.contains("Link de Pagamento");
        boolean isDebito = method.contains("Débito") || method.contains("Debito");

        if (isCredito) {
            // Regra de ouro do cartão de crédito: sempre 1 única conta a receber.
            // A modal de "Receber" (FinanceService) é quem, no momento do recebimento,
            // aplica a taxa variável da maquininha sobre este valor bruto.
            plan.add(new PlannedInstallment(totalValue, deliveryDate, STAGE_UNICO));
            return plan;
        }

        if (isDebito) {
            // Pagamento integral, cai na conta praticamente no mesmo dia da transação.
            plan.add(new PlannedInstallment(totalValue, deliveryDate, STAGE_UNICO));
            return plan;
        }

        // PIX ou Dinheiro: respeita o plano escolhido.
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
