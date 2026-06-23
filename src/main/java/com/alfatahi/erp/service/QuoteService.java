package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.repository.QuoteRepository;
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
    public void approveAndGenerateIntegrations(UUID quoteId) {
        Quote quote = quoteRepo.findById(quoteId).orElseThrow();

        if ("approved".equals(quote.getStatus())) return;
        quote.setStatus("approved");

        WorkOrder os = new WorkOrder();
        long nextOsNumber = osRepo.count() + 1;
        os.setNumber(String.format("OS-%02d", nextOsNumber));

        os.setClient(quote.getClient());
        os.setTitle("Venda: " + quote.getNumber());
        os.setDescription("Gerado via Orçamento. OBS: " + quote.getObservations());
        os.setStatus("in_progress");
        os.setTotalValue(quote.getTotalValue());

        // Converte as medidas do Comercial em Linhas Genéricas para a Gestão
        for (QuoteItem qi : quote.getItems()) {
            WorkOrderItem osItem = new WorkOrderItem();
            String dimensions = " (LxA: " + qi.getWidth() + "x" + qi.getHeight() + ")";
            osItem.setDescription(qi.getCategory() + " - " + qi.getProduct() + dimensions);
            osItem.setQuantity(qi.getQuantity());
            osItem.setUnitPrice(qi.getUnitPrice());
            osItem.setUnitCost(BigDecimal.ZERO); // Fica a zero para o Gestor preencher o CMV real dps
            osItem.setWorkOrder(os);
            os.getItems().add(osItem);
        }
        osRepo.save(os);
        quote.setWorkOrder(os); // Amarração bidirecional!

        // ==========================================
        // 2. INTEGRAÇÃO FINANCEIRA
        // ==========================================
        AccountsReceivable rec = new AccountsReceivable();
        rec.setClient(quote.getClient());
        rec.setWorkOrder(os);
        rec.setDescription("Ref. Orçamento " + quote.getNumber());
        rec.setTotalAmount(quote.getTotalValue());
        rec.setInstallments(quote.getInstallments());
        rec.setDueDate(LocalDate.now().plusDays(3)); // Pode ser ajustado
        rec.setStatus("pending");
        finRepo.save(rec);

        quoteRepo.save(quote);
    }
}