package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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

        // Mantém a inteligência de trocar ORC- por OS-
        String osNumber = quote.getNumber() != null ? quote.getNumber().replace("ORC-", "OS-") : "OS-NOVO";
        os.setNumber(osNumber);

        os.setClient(quote.getClient());
        os.setTitle("Venda: " + quote.getNumber());
        os.setDescription("Gerado via Orçamento. OBS: " + quote.getObservations());
        os.setStatus("in_progress");
        os.setTotalValue(quote.getTotalValue());

        if (quote.getItems() != null) {
            for (QuoteItem qi : quote.getItems()) {
                WorkOrderItem osItem = new WorkOrderItem();
                String dimensions = " (LxA: " + qi.getWidth() + "x" + qi.getHeight() + ")";
                osItem.setDescription(qi.getCategory() + " - " + qi.getProduct() + dimensions);

                osItem.setQuantity(qi.getQuantity());
                osItem.setUnitPrice(qi.getUnitPrice());
                osItem.setUnitCost(BigDecimal.ZERO);

                os.getItems().add(osItem);
            }
        }

        os = osRepo.saveAndFlush(os);


        quote.setStatus("approved");
        quote.setWorkOrder(os);
        quoteRepo.saveAndFlush(quote);


        AccountsReceivable rec = new AccountsReceivable();
        rec.setClient(quote.getClient());
        rec.setWorkOrder(os);
        rec.setDescription("Ref. Orçamento " + quote.getNumber());
        rec.setTotalAmount(quote.getTotalValue());
        rec.setInstallments(quote.getInstallments());
        rec.setDueDate(LocalDate.now().plusDays(3)); // Prazo de compensação
        rec.setStatus("pending");

        finRepo.save(rec);
    }
}