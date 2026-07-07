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
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepo;
    private final WorkOrderRepository osRepo;
    private final AccountsReceivableRepository finRepo;
    private final ScheduleService scheduleService;

    public QuoteService(QuoteRepository quoteRepo, WorkOrderRepository osRepo, AccountsReceivableRepository finRepo, ScheduleService scheduleService) {
        this.quoteRepo = quoteRepo;
        this.osRepo = osRepo;
        this.finRepo = finRepo;
        this.scheduleService = scheduleService;
    }

    /**
     * Calcula a área (m²) de um item a partir de largura x altura.
     * Segue a MESMA regra usada no frontend (quotes.html / work-orders.html):
     * se não houver largura/altura informadas (item vendido por unidade, não por m²),
     * o multiplicador de área deve ser 1 (neutro), e não 0.
     */
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
        Quote quote = quoteRepo.findById(quoteId).orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));

        if (!"pending".equals(quote.getStatus()) && !"sent".equals(quote.getStatus())) {
            throw new IllegalStateException("Orçamento não pode ser aprovado no status atual: " + quote.getStatus());
        }

        WorkOrder os = new WorkOrder();
        String osNumber = quote.getNumber() != null ? quote.getNumber().replace("ORC-", "OS-") : "OS-NOVO";
        os.setNumber(osNumber);
        os.setClient(quote.getClient());
        os.setTitle("Venda: " + quote.getNumber());

        String description = "Gerado via Orçamento";
        if (quote.getObservations() != null && !quote.getObservations().isBlank()) {
            description += ". OBS: " + quote.getObservations();
        }
        if (description.length() > 255) {
            description = description.substring(0, 255);
        }

        os.setDescription(description);
        os.setStatus("in_progress");

        BigDecimal subtotal = BigDecimal.ZERO;
        if (quote.getItems() != null && !quote.getItems().isEmpty()) {
            for (QuoteItem item : quote.getItems()) {
                BigDecimal area = calcularAreaM2(item.getWidth(), item.getHeight());
                BigDecimal precoUnitarioComArea = item.getUnitPrice().multiply(area);
                BigDecimal itemTotal = item.getQuantity().multiply(precoUnitarioComArea);
                subtotal = subtotal.add(itemTotal);
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (quote.getDiscountPercent() != null && quote.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            discountAmount = subtotal.multiply(quote.getDiscountPercent())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }

        BigDecimal finalTotal = subtotal.subtract(discountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        os.setTotalValue(finalTotal);

        LocalDate dataEntrega = calculateBusinessDays(LocalDate.now(), 15);
        os.setInstallDate(dataEntrega);

        if (quote.getItems() != null) {
            for (QuoteItem qi : quote.getItems()) {
                WorkOrderItem osItem = new WorkOrderItem();
                BigDecimal w = qi.getWidth() != null ? qi.getWidth() : BigDecimal.ZERO;
                BigDecimal h = qi.getHeight() != null ? qi.getHeight() : BigDecimal.ZERO;
                String dimensions = (w.compareTo(BigDecimal.ZERO) > 0 || h.compareTo(BigDecimal.ZERO) > 0)
                        ? " (LxA: " + w + "x" + h + ")"
                        : "";
                String cat = qi.getCategory() != null ? qi.getCategory() : "Item";
                String prod = qi.getProduct() != null ? qi.getProduct() : "Sem descrição";
                osItem.setDescription(cat + " - " + prod + dimensions);
                osItem.setQuantity(qi.getQuantity());
                // FIX: o preço do item do orçamento é POR M², então é preciso
                // incorporar a área (largura x altura) no preço unitário da OS,
                // igual ao que já é feito no cálculo do orçamento (frontend).
                // Sem isso, quantity * unitPrice ignora o m² e gera valor bem menor.
                BigDecimal area = calcularAreaM2(w, h);
                osItem.setUnitPrice(qi.getUnitPrice().multiply(area));
                osItem.setUnitCost(BigDecimal.ZERO);
                osItem.setWorkOrder(os);

                if (os.getItems() == null) {
                    os.setItems(new ArrayList<>());
                }
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

        String paymentMethod = quote.getPaymentMethod();
        boolean isCardSale = "Cartao_Credito".equals(paymentMethod) || "Cartao_Debito".equals(paymentMethod);

        int numParcelas = isCardSale
                ? 1
                : (quote.getInstallments() != null && quote.getInstallments() > 0) ? quote.getInstallments() : 1;

        BigDecimal valorParcela = finalTotal.divide(new BigDecimal(numParcelas), 2, RoundingMode.HALF_UP);
        BigDecimal somaParcelasAnteriores = valorParcela.multiply(new BigDecimal(numParcelas - 1));
        BigDecimal valorUltimaParcela = finalTotal.subtract(somaParcelasAnteriores);

        for (int i = 0; i < numParcelas; i++) {
            boolean isUltima = (i == numParcelas - 1);
            AccountsReceivable parcela = new AccountsReceivable();
            parcela.setClient(quote.getClient());
            parcela.setWorkOrder(os);
            parcela.setPaymentMethod(paymentMethod);
            parcela.setDescription(numParcelas == 1
                    ? "Ref. " + quote.getNumber()
                    : "Ref. " + quote.getNumber() + " — Parcela " + (i+1) + "/" + numParcelas);
            parcela.setTotalAmount(isUltima ? valorUltimaParcela : valorParcela);
            parcela.setDueDate(
                    numParcelas == 1
                            ? LocalDate.now()
                            : LocalDate.now().plusMonths(i + 1)
            );
            parcela.setStatus("pending");
            finRepo.save(parcela);
        }
    }
}