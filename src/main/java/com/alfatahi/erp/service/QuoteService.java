package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        os = osRepo.saveAndFlush(os);
        os.setQuote(quote);

        quote.setStatus("approved");
        quote.setWorkOrder(os);
        quoteRepo.saveAndFlush(quote);

        BigDecimal totalOrcamento = (quote.getTotalValue() != null) ? quote.getTotalValue() : BigDecimal.ZERO;

        int numParcelas = (quote.getInstallments() != null && quote.getInstallments() > 0) ? quote.getInstallments() : 1;
        BigDecimal valorParcela = totalOrcamento.divide(new BigDecimal(numParcelas), 2, RoundingMode.HALF_UP);

        for (int i = 0; i < numParcelas; i++) {
            AccountsReceivable parcela = new AccountsReceivable();
            parcela.setClient(quote.getClient());
            parcela.setWorkOrder(os);
            parcela.setDescription("Ref. " + quote.getNumber() + " — Parcela " + (i+1) + "/" + numParcelas);
            parcela.setTotalAmount(valorParcela);
            parcela.setDueDate(LocalDate.now().plusMonths(i + 1));
            parcela.setStatus("pending");
            finRepo.save(parcela);
        }
    }
}