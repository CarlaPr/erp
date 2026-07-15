package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepo;
    private final WorkOrderRepository osRepo;
    private final AccountsReceivableRepository finRepo;
    private final ScheduleService scheduleService;
    private final PaymentTermsService paymentTermsService;

    public QuoteService(QuoteRepository quoteRepo, WorkOrderRepository osRepo, AccountsReceivableRepository finRepo, ScheduleService scheduleService, PaymentTermsService paymentTermsService) {
        this.quoteRepo = quoteRepo;
        this.osRepo = osRepo;
        this.finRepo = finRepo;
        this.scheduleService = scheduleService;
        this.paymentTermsService = paymentTermsService;
    }

    private BigDecimal calcularAreaM2(BigDecimal width, BigDecimal height) {
        BigDecimal w = width != null ? width : BigDecimal.ZERO;
        BigDecimal h = height != null ? height : BigDecimal.ZERO;
        if (w.compareTo(BigDecimal.ZERO) > 0 && h.compareTo(BigDecimal.ZERO) > 0) {
            return w.multiply(h);
        }
        return BigDecimal.ONE;
    }

    private LocalDate calculateBusinessDays(LocalDate startDate, int businessDays) {
        LocalDate result = startDate;
        int addedDays = 0;
        while (addedDays < businessDays) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                addedDays++;
            }
        }
        return result;
    }

    @Transactional
    public void approveQuote(UUID quoteId) {
        Quote quote = quoteRepo.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));

        if (!"pending".equals(quote.getStatus()) && !"sent".equals(quote.getStatus())) {
            throw new IllegalStateException(
                    "Orçamento não pode ser aprovado no status atual: " + quote.getStatus());
        }

        WorkOrder os = new WorkOrder();
        String osNumber = quote.getNumber() != null
                ? quote.getNumber().replace("ORC-", "OS-")
                : "OS-NOVO";
        os.setNumber(osNumber);
        os.setClient(quote.getClient());
        os.setTitle("Venda: " + quote.getNumber());

        String description = "Gerado via Orçamento";
        if (quote.getObservations() != null && !quote.getObservations().isBlank()) {
            description += ". OBS: " + quote.getObservations();
        }
        if (description.length() > 255) description = description.substring(0, 255);
        os.setDescription(description);
        os.setStatus("in_progress");

        BigDecimal subtotal = BigDecimal.ZERO;
        if (quote.getItems() != null && !quote.getItems().isEmpty()) {
            for (QuoteItem item : quote.getItems()) {
                BigDecimal area = calcularAreaM2(item.getWidth(), item.getHeight());
                BigDecimal precoComArea = item.getUnitPrice().multiply(area);
                subtotal = subtotal.add(item.getQuantity().multiply(precoComArea));
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (quote.getDiscountPercent() != null
                && quote.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = subtotal.multiply(quote.getDiscountPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        BigDecimal finalTotal = subtotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;
        os.setTotalValue(finalTotal);

        LocalDate dataEntrega = calculateBusinessDays(LocalDate.now(), 15);
        os.setInstallDate(dataEntrega);

        if (quote.getItems() != null) {
            for (QuoteItem qi : quote.getItems()) {
                WorkOrderItem osItem = new WorkOrderItem();
                BigDecimal w = qi.getWidth() != null ? qi.getWidth() : BigDecimal.ZERO;
                BigDecimal h = qi.getHeight() != null ? qi.getHeight() : BigDecimal.ZERO;
                String dimensions = (w.compareTo(BigDecimal.ZERO) > 0 || h.compareTo(BigDecimal.ZERO) > 0)
                        ? " (LxA: " + w + "x" + h + ")" : "";
                String cat = qi.getCategory() != null ? qi.getCategory() : "Item";
                String prod = qi.getProduct() != null ? qi.getProduct() : "Sem descrição";
                osItem.setDescription(cat + " - " + prod + dimensions);
                osItem.setQuantity(qi.getQuantity());
                BigDecimal area = calcularAreaM2(w, h);
                osItem.setUnitPrice(qi.getUnitPrice().multiply(area));
                osItem.setUnitCost(BigDecimal.ZERO);
                osItem.setWorkOrder(os);
                if (os.getItems() == null) os.setItems(new ArrayList<>());
                os.getItems().add(osItem);
            }
        }

        quote.setStatus("approved");
        quote.setDateApproved(LocalDateTime.now());
        os.setQuote(quote);
        quote.setWorkOrder(os);

        os = osRepo.saveAndFlush(os);
        quoteRepo.saveAndFlush(quote);
        scheduleService.createFromApprovedQuote(quote, os);

        String paymentMethod = quote.getPaymentMethod() != null ? quote.getPaymentMethod() : "";
        String paymentPlan   = quote.getPaymentPlan();

        // Verifica se a forma de pagamento se enquadra na regra de 50/50 obrigatória
        String pmUpper = paymentMethod.toUpperCase();
        boolean forceSplit = pmUpper.contains("PIX") || pmUpper.contains("DINHEIRO") || pmUpper.contains("DÉBITO") || pmUpper.contains("DEBITO");

        if (forceSplit) {
            // Separa os valores rigorosamente em 50%
            BigDecimal entrada = finalTotal.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            BigDecimal saldo = finalTotal.subtract(entrada);

            // 1. Recebível de Entrada (Vencimento hoje)
            AccountsReceivable rec1 = new AccountsReceivable();
            rec1.setClient(quote.getClient());
            rec1.setWorkOrder(os);
            rec1.setPaymentMethod(paymentMethod);
            rec1.setPaymentStage(PaymentTermsService.STAGE_ENTRADA);
            rec1.setReferenceMonth(LocalDate.now().withDayOfMonth(1));
            rec1.setDescription("Ref. " + quote.getNumber() + " — Entrada (50%)");
            rec1.setTotalAmount(entrada);
            rec1.setInstallments(2);
            rec1.setDueDate(LocalDate.now()); // Vencimento no ato da aprovação
            rec1.setStatus("pending");
            finRepo.save(rec1);

            // 2. Recebível de Entrega (Vencimento na data da instalação)
            AccountsReceivable rec2 = new AccountsReceivable();
            rec2.setClient(quote.getClient());
            rec2.setWorkOrder(os);
            rec2.setPaymentMethod(paymentMethod);
            rec2.setPaymentStage(PaymentTermsService.STAGE_ENTREGA);
            rec2.setReferenceMonth(dataEntrega.withDayOfMonth(1));
            rec2.setDescription("Ref. " + quote.getNumber() + " — Saldo na Entrega (50%)");
            rec2.setTotalAmount(saldo);
            rec2.setInstallments(2);
            rec2.setDueDate(dataEntrega); // Vencimento na entrega
            rec2.setStatus("pending");
            finRepo.save(rec2);

        } else {
            // Lógica padrão para Crédito ou outras formas
            List<PaymentTermsService.PlannedInstallment> installments =
                    paymentTermsService.generateInstallments(
                            paymentMethod, paymentPlan, finalTotal, LocalDate.now(), dataEntrega);

            int total = installments.size();
            for (int i = 0; i < total; i++) {
                PaymentTermsService.PlannedInstallment inst = installments.get(i);
                AccountsReceivable receivable = new AccountsReceivable();
                receivable.setClient(quote.getClient());
                receivable.setWorkOrder(os);
                receivable.setPaymentMethod(paymentMethod);
                receivable.setPaymentStage(inst.getStage());
                receivable.setReferenceMonth(inst.getDueDate().withDayOfMonth(1));

                String desc;
                if (total == 1) {
                    desc = "Ref. " + quote.getNumber();
                } else {
                    String stageLabel = PaymentTermsService.STAGE_ENTRADA.equals(inst.getStage())
                            ? "Entrada (50%)" : "Saldo na Entrega (50%)";
                    desc = "Ref. " + quote.getNumber() + " — " + stageLabel;
                }
                receivable.setDescription(desc);
                receivable.setTotalAmount(inst.getAmount());
                receivable.setInstallments(total);
                receivable.setDueDate(inst.getDueDate());
                receivable.setStatus("pending");
                finRepo.save(receivable);
            }
        }
    }

    @Transactional
    public void deleteQuote(UUID quoteId) {
        Quote quote = quoteRepo.findById(quoteId).orElse(null);
        if (quote != null) {
            if (quote.getWorkOrder() != null) {
                WorkOrder os = quote.getWorkOrder();
                os.setQuote(null);
                osRepo.save(os);
            }
            scheduleService.onQuoteCancelled(quoteId);
            quoteRepo.delete(quote);
        }
    }
}