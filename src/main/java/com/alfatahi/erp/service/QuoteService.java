package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.util.DeliveryDeadline;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepo;
    private final WorkOrderRepository osRepo;
    private final ScheduleService scheduleService;
    private final TechnicalVisitService technicalVisitService;

    public QuoteService(QuoteRepository quoteRepo, WorkOrderRepository osRepo, ScheduleService scheduleService,
                         TechnicalVisitService technicalVisitService) {
        this.quoteRepo = quoteRepo;
        this.osRepo = osRepo;
        this.scheduleService = scheduleService;
        this.technicalVisitService = technicalVisitService;
    }

    private BigDecimal calcularAreaM2(BigDecimal width, BigDecimal height) {
        BigDecimal w = width != null ? width : BigDecimal.ZERO;
        BigDecimal h = height != null ? height : BigDecimal.ZERO;
        if (w.compareTo(BigDecimal.ZERO) > 0 && h.compareTo(BigDecimal.ZERO) > 0) {
            return w.multiply(h);
        }
        return BigDecimal.ONE;
    }

    @Transactional
    public void approveQuote(UUID quoteId) {
        Quote quote = quoteRepo.findByIdForUpdate(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        if (!"pending".equals(quote.getStatus()) && !"sent".equals(quote.getStatus())
                && !"expired".equals(quote.getStatus()) && !"approved".equals(quote.getStatus())) {
            throw new IllegalStateException(
                    "Orçamento não pode ser aprovado no status atual: " + quote.getStatus());
        }

        ensureApprovedWorkOrder(quote);
    }

    @Transactional
    public WorkOrder recoverApprovedWorkOrder(UUID quoteId) {
        Quote quote = quoteRepo.findByIdForUpdate(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));
        if (!"approved".equals(quote.getStatus())) {
            throw new IllegalStateException("Somente orçamentos aprovados podem ter a O.S. recuperada.");
        }
        return ensureApprovedWorkOrder(quote);
    }

    private WorkOrder ensureApprovedWorkOrder(Quote quote) {
        boolean alreadyApproved = "approved".equals(quote.getStatus());
        WorkOrder existing = osRepo.findByQuoteId(quote.getId())
                .orElseGet(() -> scheduleService.findWorkOrderByQuoteId(quote.getId()).orElse(null));
        if (existing != null) {
            if (existing.getQuote() != null && !quote.getId().equals(existing.getQuote().getId())) {
                throw new IllegalStateException("A O.S. da agenda está vinculada a outro orçamento. Revise os vínculos antes de continuar.");
            }
            if (!alreadyApproved && ("cancelled".equals(existing.getStatus()) || "canceled".equals(existing.getStatus()))) {
                throw new IllegalStateException("A O.S. deste orçamento está cancelada.");
            }
            // A agenda identifica a O.S. mesmo quando o vínculo antigo foi removido.
            // Não inferimos vínculos pelo número nem reativamos ordens canceladas.
            if (existing.getQuote() == null) {
                existing.setQuote(quote);
                osRepo.saveAndFlush(existing);
            }
            quote.setWorkOrder(existing);
            if (!alreadyApproved) {
                quote.setStatus("approved");
                quote.setDateApproved(LocalDateTime.now());
                quoteRepo.saveAndFlush(quote);
                scheduleService.createFromApprovedQuote(quote, existing);
            }
            return existing;
        }

        LocalDateTime approvalDate = alreadyApproved
                ? (quote.getDateApproved() != null ? quote.getDateApproved() : quote.getDateCreated())
                : LocalDateTime.now();
        if (approvalDate == null) {
            throw new IllegalStateException("Orçamento sem data de aprovação ou emissão. Corrija a data antes de recuperar a O.S.");
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
        os.setCreatedAt(approvalDate);

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
        os.setTotalValue(alreadyApproved && quote.getTotalValue() != null ? quote.getTotalValue() : finalTotal);

        os.setDeadlineDate(DeliveryDeadline.fromApproval(approvalDate));

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
        quote.setDateApproved(approvalDate);
        os.setQuote(quote);
        quote.setWorkOrder(os);

        os = osRepo.saveAndFlush(os);
        quoteRepo.saveAndFlush(quote);
        scheduleService.createFromApprovedQuote(quote, os);
        return os;
    }

    @Transactional
    public void resetClientSignature(UUID quoteId) {
        Quote quote = quoteRepo.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado"));

        quote.setClientSignature(null);
        quoteRepo.save(quote);
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
            technicalVisitService.onQuoteDeleted(quoteId);
            quoteRepo.delete(quote);
        }
    }

    /**
     * Exclui definitivamente os orçamentos já marcados como "expired" cuja data de emissão
     * (dateCreated) seja anterior ao limite informado. Reaproveita deleteQuote para garantir
     * que vínculos (OS, agenda, visitas técnicas) sejam desfeitos de forma consistente.
     *
     * IMPORTANTE: somente orçamentos NÃO aprovados (status "expired") podem ser excluídos por
     * esta rotina. Orçamentos aprovados devem permanecer no sistema indefinidamente — por isso,
     * além da consulta já filtrar por status = 'expired', o laço abaixo ignora explicitamente
     * qualquer registro com status "approved" como segunda camada de proteção.
     */
    @Transactional
    public int purgeExpiredQuotesOlderThan(LocalDateTime limite) {
        List<Quote> expiradosParaExcluir = quoteRepo.findExpiredQuotesOlderThan(limite);
        int excluidos = 0;
        for (Quote quote : expiradosParaExcluir) {
            if ("approved".equals(quote.getStatus())) {
                continue;
            }
            deleteQuote(quote.getId());
            excluidos++;
        }
        return excluidos;
    }
}