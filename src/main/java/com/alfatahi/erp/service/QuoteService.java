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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QuoteService {

    public static final int DEFAULT_VALIDITY_DAYS = 15;

    private final QuoteRepository quoteRepo;
    private final WorkOrderRepository osRepo;
    private final ScheduleService scheduleService;
    private final TechnicalVisitService technicalVisitService;
    private final WorkOrderService workOrderService;
    private final QuoteCommercialConditionsService commercialConditionsService;

    public QuoteService(QuoteRepository quoteRepo, WorkOrderRepository osRepo, ScheduleService scheduleService,
                         TechnicalVisitService technicalVisitService, WorkOrderService workOrderService,
                         QuoteCommercialConditionsService commercialConditionsService) {
        this.quoteRepo = quoteRepo;
        this.osRepo = osRepo;
        this.scheduleService = scheduleService;
        this.technicalVisitService = technicalVisitService;
        this.workOrderService = workOrderService;
        this.commercialConditionsService = commercialConditionsService;
    }

    @Transactional
    public void approveQuote(UUID quoteId) {
        approveQuote(quoteId, Map.of());
    }

    @Transactional
    public void approveQuote(UUID quoteId, Map<String, BigDecimal> paymentComposition) {
        approveQuote(quoteId, paymentComposition, null);
    }

    @Transactional
    public void approveQuote(UUID quoteId, Map<String, BigDecimal> paymentComposition, Integer agreedCreditInstallments) {
        Quote quote = quoteRepo.findByIdForUpdate(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado"));

        if (!"pending".equals(quote.getStatus()) && !"sent".equals(quote.getStatus())
                && !"expired".equals(quote.getStatus()) && !"approved".equals(quote.getStatus())) {
            throw new IllegalStateException(
                    "Orçamento não pode ser aprovado no status atual: " + quote.getStatus());
        }

        if (!"approved".equals(quote.getStatus()) && paymentComposition != null && !paymentComposition.isEmpty()) {
            commercialConditions(quote);
            Map<String, BigDecimal> normalizedPayments = new java.util.LinkedHashMap<>();
            paymentComposition.forEach((method, amount) -> {
                if (method == null || amount == null) throw new IllegalArgumentException("Informe uma forma e um valor de pagamento válidos.");
                normalizedPayments.put(method, amount.setScale(2, RoundingMode.HALF_UP));
            });
            boolean cardSelected = normalizedPayments.keySet().stream().anyMatch(QuoteService::isCreditPaymentMethod);
            if (cardSelected && (agreedCreditInstallments == null || agreedCreditInstallments < 1 || agreedCreditInstallments > 10)) {
                throw new IllegalArgumentException("Informe a quantidade de parcelas do cartão entre 1 e 10.");
            }
            BigDecimal due = resolveApprovedSaleTotal(quote, normalizedPayments);
            quote.setPaymentComposition(normalizedPayments);
            quote.setApprovedSaleTotal(due);
            quote.setAgreedCreditInstallments(cardSelected ? agreedCreditInstallments : null);
            quote.setAgreedCreditWithInterest(quote.getCreditWithInterest());
            quote.setAgreedCreditMonthlyInterestRate(quote.getCreditMonthlyInterestRate());
        }

        ensureApprovedWorkOrder(quote);
    }

    private static boolean isCreditPaymentMethod(String method) {
        return "Crédito".equalsIgnoreCase(method) || "Credito".equalsIgnoreCase(method)
                || "Link de Pagamento".equalsIgnoreCase(method);
    }

    public QuoteCommercialConditionsService.Calculation commercialConditions(Quote quote) {
        normalizeLegacyCreditTerms(quote);
        commercialConditionsService.applyAndValidate(quote);
        return commercialConditionsService.calculate(quote);
    }

    private void normalizeLegacyCreditTerms(Quote quote) {
        if (quote == null || quote.getCreditMonthlyInterestRate() != null || quote.getCreditInstallments() != 0
                || quote.getInstallments() == null || quote.getInstallments() < 1 || quote.getInstallments() > 10) return;
        String methods = quote.getPaymentMethod() == null ? "" : quote.getPaymentMethod().toLowerCase(java.util.Locale.ROOT);
        if (methods.contains("crédito") || methods.contains("credito") || methods.contains("link de pagamento")) {
            quote.setCreditInstallments(quote.getInstallments());
        }
    }

    public BigDecimal resolveApprovedSaleTotal(Quote quote, Map<String, BigDecimal> payments) {
        if (payments == null || payments.isEmpty()) return computeFinalTotal(quote);
        List<String> available = quote.getPaymentMethod() == null ? List.of()
                : java.util.Arrays.stream(quote.getPaymentMethod().split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toList();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> payment : payments.entrySet()) {
            if (payment.getKey() == null || payment.getValue() == null || payment.getValue().signum() <= 0) {
                throw new IllegalArgumentException("Informe um valor maior que zero para cada forma selecionada.");
            }
            if (!available.isEmpty() && !available.contains(payment.getKey())) {
                throw new IllegalArgumentException("A forma " + payment.getKey() + " não está habilitada neste orçamento.");
            }
            sum = sum.add(payment.getValue().setScale(2, RoundingMode.HALF_UP));
        }
        sum = sum.setScale(2, RoundingMode.HALF_UP);

        QuoteCommercialConditionsService.Calculation conditions = commercialConditionsService.calculate(quote);
        BigDecimal expected = conditions.baseSaleTotal();
        if (payments.size() == 1) {
            String method = payments.keySet().iterator().next();
            if ("Dinheiro".equalsIgnoreCase(method)) expected = conditions.cashFinalTotal();
            else if ("PIX".equalsIgnoreCase(method)) expected = conditions.pixFinalTotal();
            else if ("Boleto".equalsIgnoreCase(method) || "Boleto Bancário".equalsIgnoreCase(method)) {
                if (conditions.boletoEnabled()) expected = conditions.boletoTotal();
            }
        }
        if (sum.compareTo(expected) != 0) {
            throw new IllegalArgumentException("A soma das formas de pagamento deve ser " + expected.setScale(2, RoundingMode.HALF_UP) + ".");
        }
        return expected;
    }

    @Transactional
    public void updateApprovedPaymentComposition(Quote quote, Map<String, BigDecimal> paymentComposition,
                                                 Integer agreedCreditInstallments) {
        if (paymentComposition == null || paymentComposition.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos uma modalidade de pagamento aprovada.");
        }

        commercialConditions(quote);
        Map<String, BigDecimal> normalizedPayments = new java.util.LinkedHashMap<>();
        paymentComposition.forEach((method, amount) -> {
            if (method == null || amount == null) {
                throw new IllegalArgumentException("Informe uma modalidade e um valor de pagamento válidos.");
            }
            normalizedPayments.put(method, amount.setScale(2, RoundingMode.HALF_UP));
        });

        boolean cardSelected = normalizedPayments.keySet().stream().anyMatch(QuoteService::isCreditPaymentMethod);
        if (cardSelected && (agreedCreditInstallments == null
                || agreedCreditInstallments < 1 || agreedCreditInstallments > 10)) {
            throw new IllegalArgumentException("Informe as parcelas do cartão entre 1 e 10.");
        }

        BigDecimal approvedTotal = resolveApprovedSaleTotal(quote, normalizedPayments);
        quote.setPaymentComposition(normalizedPayments);
        quote.setApprovedSaleTotal(approvedTotal);
        quote.setAgreedCreditInstallments(cardSelected ? agreedCreditInstallments : null);
        quote.setAgreedCreditWithInterest(cardSelected && quote.getCreditWithInterest());
        quote.setAgreedCreditMonthlyInterestRate(cardSelected ? quote.getCreditMonthlyInterestRate() : null);
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

    @Transactional
    public void reopenAfterWorkOrderDeletion(Quote quote) {
        if (quote == null) return;

        quote.setWorkOrder(null);
        quote.setStatus("pending");
        quote.setDateApproved(null);
        quote.setApprovedSaleTotal(null);
        quote.setPaymentComposition(Map.of());
        quote.setAgreedCreditInstallments(null);
        quote.setAgreedCreditWithInterest(null);
        quote.setAgreedCreditMonthlyInterestRate(null);
        quoteRepo.saveAndFlush(quote);
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
            if (existing.getQuote() == null) {
                existing.setQuote(quote);
                osRepo.saveAndFlush(existing);
            }
            applySiteDefaults(existing, quote.getClient());
            quote.setWorkOrder(existing);
            if (!alreadyApproved) {
                BigDecimal oldTotal = existing.getTotalValue() != null ? existing.getTotalValue() : BigDecimal.ZERO;
                BigDecimal saleTotal = quote.getApprovedSaleTotal() != null ? quote.getApprovedSaleTotal() : computeFinalTotal(quote);
                existing.setTotalValue(saleTotal);
                osRepo.saveAndFlush(existing);
                if (oldTotal.compareTo(saleTotal) != 0) {
                    workOrderService.redistributeReceivablesIfTotalChanged(existing, oldTotal,
                            "Valor ajustado conforme forma de pagamento do orçamento " + quote.getNumber() + ".");
                }
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
        applySiteDefaults(os, quote.getClient());
        os.setTitle("Venda: " + quote.getNumber());

        String description = "Gerado via Orçamento";
        if (quote.getObservations() != null && !quote.getObservations().isBlank()) {
            description += ". OBS: " + quote.getObservations();
        }
        if (description.length() > 255) description = description.substring(0, 255);
        os.setDescription(description);
        os.setStatus("in_progress");
        os.setCreatedAt(approvalDate);

        BigDecimal finalTotal = computeFinalTotal(quote);
        os.setTotalValue(alreadyApproved && quote.getTotalValue() != null ? quote.getTotalValue()
                : (quote.getApprovedSaleTotal() != null ? quote.getApprovedSaleTotal() : finalTotal));

        os.setDeadlineDate(DeliveryDeadline.fromApproval(approvalDate));

        os.setItems(buildWorkOrderItems(quote, os));

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
    public WorkOrder syncApprovedQuoteToWorkOrder(Quote quote) {
        if (quote == null || !"approved".equals(quote.getStatus())) {
            return null;
        }

        WorkOrder os = osRepo.findByQuoteId(quote.getId()).orElse(null);
        if (os == null) {
            return null;
        }

        BigDecimal oldTotal = os.getTotalValue();

        os.setClient(quote.getClient());

        Map<String, List<String>> previousNotes = new HashMap<>();
        for (WorkOrderItem previous : os.getItems()) {
            previousNotes.computeIfAbsent(previous.getDescription(), key -> new ArrayList<>())
                    .add(previous.getTechnicalNotes());
        }
        os.getItems().clear();
        List<WorkOrderItem> refreshedItems = buildWorkOrderItems(quote, os);
        for (WorkOrderItem item : refreshedItems) {
            List<String> notes = previousNotes.get(item.getDescription());
            if (notes != null && !notes.isEmpty()) item.setTechnicalNotes(notes.remove(0));
        }
        os.getItems().addAll(refreshedItems);

        BigDecimal finalTotal = quote.getApprovedSaleTotal() != null && quote.getApprovedSaleTotal().compareTo(BigDecimal.ZERO) > 0
                ? quote.getApprovedSaleTotal()
                : ((quote.getTotalValue() != null && quote.getTotalValue().compareTo(BigDecimal.ZERO) > 0)
                    ? quote.getTotalValue() : computeFinalTotal(quote));
        os.setTotalValue(finalTotal);

        String currentNotes = os.getNotes() != null ? os.getNotes() : "";
        os.setNotes(currentNotes + "\n[Sistema] Itens/valor atualizados pela edição do orçamento " + quote.getNumber() + ".");

        WorkOrder saved = osRepo.saveAndFlush(os);

        workOrderService.redistributeReceivablesIfTotalChanged(saved, oldTotal,
                "Valor ajustado conforme edição do orçamento " + quote.getNumber() + ".");

        return saved;
    }

    public BigDecimal computeFinalTotal(Quote quote) {
        return commercialConditionsService.calculate(quote).baseSaleTotal();
    }

    private List<WorkOrderItem> buildWorkOrderItems(Quote quote, WorkOrder os) {
        List<WorkOrderItem> items = new ArrayList<>();
        if (quote.getItems() == null) return items;
        for (QuoteItem qi : quote.getItems()) {
            WorkOrderItem osItem = new WorkOrderItem();
            String cat = qi.getCategory() != null ? qi.getCategory() : "Item";
            String prod = qi.getProduct() != null ? qi.getProduct() : "Sem descrição";
            osItem.setDescription(cat + " - " + prod);
            osItem.setQuantity(qi.getPricingQuantity());
            osItem.setUnitPrice(qi.getCalculatedUnitPrice());
            osItem.setUnitCost(BigDecimal.ZERO);
            osItem.setQuoteItemId(qi.getId());
            osItem.setTechnicalMeasurements(formatMeasurements(qi));
            osItem.setWorkOrder(os);
            items.add(osItem);
        }
        return items;
    }

    private void applySiteDefaults(WorkOrder order, Client client) {
        if (client == null) return;
        String address = client.getAddress() != null ? client.getAddress().trim() : "";
        String city = client.getCity() != null ? client.getCity().trim() : "";
        if (order.getInstallationAddress() == null || order.getInstallationAddress().isBlank()) {
            order.setInstallationAddress(address + (!address.isEmpty() && !city.isEmpty() ? " - " : "") + city);
        }
        if (order.getSiteContact() == null || order.getSiteContact().isBlank()) order.setSiteContact(client.getName());
        if (order.getSitePhone() == null || order.getSitePhone().isBlank()) order.setSitePhone(client.getPhone());
    }

    private String formatMeasurements(QuoteItem item) {
        List<String> rows = new ArrayList<>();
        if (item.getMeasurements() != null && !item.getMeasurements().isEmpty()) {
            for (QuoteItemMeasurement measurement : item.getMeasurements()) {
                if (measurement.getWidth() == null || measurement.getHeight() == null) continue;
                String row = decimal(measurement.getWidth()) + " m × " + decimal(measurement.getHeight()) + " m";
                if (measurement.getQuantity() != null && measurement.getQuantity().compareTo(BigDecimal.ONE) != 0) {
                    row += " (" + decimal(measurement.getQuantity()) + " un.)";
                }
                rows.add(row);
            }
        } else if (item.getWidth() != null && item.getHeight() != null
                && item.getWidth().signum() > 0 && item.getHeight().signum() > 0) {
            rows.add(decimal(item.getWidth()) + " m × " + decimal(item.getHeight()) + " m");
        }
        return rows.isEmpty() ? null : String.join("\n", rows);
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',');
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
