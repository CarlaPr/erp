package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepo;
    private final WorkOrderRepository osRepo;
    private final AccountsReceivableRepository finRepo;

    public QuoteService(QuoteRepository quoteRepo, WorkOrderRepository osRepo, AccountsReceivableRepository finRepo) {
        this.quoteRepo = quoteRepo;
        this.osRepo = osRepo;
        this.finRepo = finRepo;
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

        if ("approved".equals(quote.getStatus())) {
            return;
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

        os.setTotalValue(quote.getTotalValue());

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
                osItem.setUnitPrice(qi.getUnitPrice());
                osItem.setUnitCost(BigDecimal.ZERO);
                osItem.setWorkOrder(os);

                if (os.getItems() == null) {
                    os.setItems(new ArrayList<>());
                }
                os.getItems().add(osItem);
            }
        }

        quote.setStatus("approved");
        os.setQuote(quote);
        quote.setWorkOrder(os);

        os = osRepo.saveAndFlush(os);
        quoteRepo.saveAndFlush(quote);

        BigDecimal totalOrcamento = (quote.getTotalValue() != null) ? quote.getTotalValue() : BigDecimal.ZERO;

        String paymentMethod = quote.getPaymentMethod();
        boolean isCardSale = "Cartao_Credito".equals(paymentMethod) || "Cartao_Debito".equals(paymentMethod);

        int numParcelas = isCardSale
                ? 1
                : (quote.getInstallments() != null && quote.getInstallments() > 0) ? quote.getInstallments() : 1;
        BigDecimal valorParcela = totalOrcamento.divide(new BigDecimal(numParcelas), 2, RoundingMode.HALF_UP);


        BigDecimal somaParcelasAnteriores = valorParcela.multiply(new BigDecimal(numParcelas - 1));
        BigDecimal valorUltimaParcela = totalOrcamento.subtract(somaParcelasAnteriores);

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