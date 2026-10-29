package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.repository.ScheduleRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkOrderPdfService {
    public record Pdf(byte[] bytes, String fileName) { }

    private final WorkOrderRepository workOrderRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProfileRepository profileRepository;
    private final TemplateEngine templateEngine;
    private final CompanyImageService companyImageService;

    public WorkOrderPdfService(WorkOrderRepository workOrderRepository,
                               AccountsReceivableRepository receivableRepository,
                               ScheduleRepository scheduleRepository,
                               ProfileRepository profileRepository,
                               TemplateEngine templateEngine, CompanyImageService companyImageService) {
        this.workOrderRepository = workOrderRepository;
        this.receivableRepository = receivableRepository;
        this.scheduleRepository = scheduleRepository;
        this.profileRepository = profileRepository;
        this.templateEngine = templateEngine;
        this.companyImageService = companyImageService;
    }

    @Transactional(readOnly = true)
    public Pdf render(UUID id) throws Exception {
        WorkOrder order = workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada."));
        Hibernate.initialize(order.getItems());
        if (order.getClient() != null) Hibernate.initialize(order.getClient());
        if (order.getQuote() != null) {
            Hibernate.initialize(order.getQuote());
            if (order.getQuote().getProfile() != null) Hibernate.initialize(order.getQuote().getProfile());
            if (order.getQuote().getClient() != null) Hibernate.initialize(order.getQuote().getClient());
            if (!"approved".equals(order.getQuote().getStatus())) {
                throw new IllegalStateException("A ordem de serviço só pode ser impressa após a aprovação do orçamento.");
            }
        }

        Quote quote = order.getQuote();
        List<AccountsReceivable> receivables = receivableRepository.findByWorkOrderId(order.getId()).stream()
                .filter(receivable -> !"cancelled".equalsIgnoreCase(value(receivable.getStatus())))
                .toList();
        Client client = order.getClient() != null ? order.getClient()
                : quote != null ? quote.getClient() : null;
        if (client != null) Hibernate.initialize(client);

        Profile profile = order.getQuote() != null && order.getQuote().getProfile() != null
                ? order.getQuote().getProfile()
                : profileRepository.findAll().stream().findFirst().orElse(null);
        String clientName = client != null && client.getName() != null
                ? client.getName() : "Consumidor Final";
        String clientPhone = nonBlank(order.getSitePhone()) ? order.getSitePhone()
                : client != null ? value(client.getPhone()) : "";
        String siteAddress = order.getInstallationAddress();
        if (!nonBlank(siteAddress) && client != null) {
            String address = value(client.getAddress()).trim();
            String city = value(client.getCity()).trim();
            siteAddress = address + (!address.isEmpty() && !city.isEmpty() ? " - " : "") + city;
        }

        List<WorkOrderItem> services = order.getItems().stream()
                .filter(item -> item.getQuoteItemId() != null
                        || ((item.getUnitCost() == null || item.getUnitCost().signum() == 0)
                        && item.getDescription() != null && !item.getDescription().startsWith("[OP] ")))
                .toList();

        Context context = new Context(Locale.forLanguageTag("pt-BR"));
        context.setVariable("order", order);
        context.setVariable("clientName", clientName);
        context.setVariable("clientPhone", clientPhone);
        context.setVariable("siteAddress", value(siteAddress));
        context.setVariable("siteContact", value(order.getSiteContact()));
        context.setVariable("technicianName", value(order.getTechnicianName()));
        context.setVariable("accessInstructions", value(order.getAccessInstructions()));
        context.setVariable("installationNotes", value(order.getInstallationNotes()));
        context.setVariable("conditionsObservations", value(order.getConditionsObservations()));
        context.setVariable("warrantyTerms", value(order.getWarrantyTerms()));
        context.setVariable("services", services);
        context.setVariable("companyName", profile != null ? value(profile.getCompanyName()) : "");
        context.setVariable("companyDocument", profile != null ? value(profile.getDocument()) : "--");
        context.setVariable("companyPhone", profile != null ? value(profile.getPhone()) : "");
        context.setVariable("companyEmail", profile != null ? value(profile.getEmail()) : "");
        context.setVariable("companySignatureBase64", companyImageService.signatureDataUri(profile));
        context.setVariable("quoteNumber", order.getQuote() != null ? value(order.getQuote().getNumber()) : "");
        context.setVariable("paymentMethod", paymentMethodText(quote, receivables));
        context.setVariable("paymentInstallmentsText", paymentInstallmentsText(quote));
        context.setVariable("paymentStatusText", paymentStatusText(order, receivables));
        context.setVariable("paymentPaidInFull", isPaidInFull(order, receivables));
        context.setVariable("generatedDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("deadlineDate", order.getDeadlineDate() == null ? ""
                : order.getDeadlineDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("installDates", installationDatesText(order));
        context.setVariable("logoBase64", companyImageService.logoDataUri(profile));

        String html = templateEngine.process("work-order-pdf", context);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(output);
        builder.run();

        String fileName = ("Ordem de Serviço " + value(order.getNumber()) + " - " + clientName)
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "")
                .replaceAll("\\s+", " ").trim() + ".pdf";
        return new Pdf(output.toByteArray(), fileName);
    }

    @Transactional(readOnly = true)
    public Pdf renderBatch(List<UUID> ids) throws Exception {
        List<UUID> orderIds = ids == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(ids));
        if (orderIds.isEmpty() || orderIds.stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("Selecione ao menos uma ordem de serviço para imprimir.");
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        List<ByteArrayInputStream> sources = new ArrayList<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            for (UUID orderId : orderIds) {
                Pdf pdf = render(orderId);
                ByteArrayInputStream source = new ByteArrayInputStream(pdf.bytes());
                sources.add(source);
                merger.addSource(source);
            }
            merger.setDestinationStream(output);
            merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
            return new Pdf(output.toByteArray(), "Ordens-de-Servico-selecionadas.pdf");
        } finally {
            for (ByteArrayInputStream source : sources) {
                try { source.close(); } catch (Exception ignored) { }
            }
            output.close();
        }
    }

    private String paymentMethodText(Quote quote, List<AccountsReceivable> receivables) {
        List<String> receivedMethods = receivables.stream()
                .filter(this::hasReceivedAmount)
                .map(AccountsReceivable::getPaymentMethod)
                .filter(this::nonBlank)
                .distinct()
                .toList();
        if (!receivedMethods.isEmpty()) return formatPaymentMethods(receivedMethods);

        if (quote != null && nonBlank(quote.getPaymentMethod())) {
            return formatPaymentMethods(java.util.Arrays.asList(quote.getPaymentMethod().split(",")));
        }

        List<String> receivableMethods = receivables.stream()
                .map(AccountsReceivable::getPaymentMethod)
                .filter(this::nonBlank)
                .distinct()
                .toList();
        return receivableMethods.isEmpty() ? "Não informado" : formatPaymentMethods(receivableMethods);
    }

    private String formatPaymentMethods(List<String> methods) {
        String formattedMethods = methods.stream()
                .map(String::trim)
                .filter(method -> !method.isEmpty())
                .map(this::paymentMethodLabel)
                .distinct()
                .collect(Collectors.joining(", "));
        return formattedMethods.isEmpty() ? "Não informado" : formattedMethods;
    }

    private String paymentMethodLabel(String method) {
        return switch (method.toLowerCase(Locale.ROOT)) {
            case "débito", "debito" -> "Cartão de Débito";
            case "crédito", "credito" -> "Cartão de Crédito";
            case "card" -> "Cartão";
            case "pix" -> "PIX";
            case "boleto", "boleto bancário", "boleto bancario" -> "Boleto Bancário";
            case "cash", "dinheiro" -> "Dinheiro";
            case "transfer", "transferência", "transferencia", "transferência bancária", "transferencia bancaria" -> "Transferência Bancária";
            default -> method;
        };
    }

    private String paymentStatusText(WorkOrder order, List<AccountsReceivable> receivables) {
        BigDecimal total = paymentTotal(order, receivables);
        BigDecimal paid = totalPaid(receivables);
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

        if (total.signum() > 0 && paid.compareTo(total) >= 0) {
            return "PAGO INTEGRALMENTE — " + currency.format(total);
        }
        if (paid.signum() > 0) {
            BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);
            return "Pago: " + currency.format(paid) + " | Restante: " + currency.format(remaining);
        }
        return "A pagar: " + currency.format(total);
    }

    private boolean isPaidInFull(WorkOrder order, List<AccountsReceivable> receivables) {
        BigDecimal total = paymentTotal(order, receivables);
        return total.signum() > 0 && totalPaid(receivables).compareTo(total) >= 0;
    }

    private BigDecimal paymentTotal(WorkOrder order, List<AccountsReceivable> receivables) {
        BigDecimal orderTotal = order.getTotalValue();
        if (orderTotal.signum() > 0) return orderTotal;
        return receivables.stream()
                .map(AccountsReceivable::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalPaid(List<AccountsReceivable> receivables) {
        return receivables.stream()
                .map(this::receivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal receivedAmount(AccountsReceivable receivable) {
        if ("received".equalsIgnoreCase(value(receivable.getStatus()))) {
            return receivable.getTotalAmount();
        }
        BigDecimal grossReceived = receivable.getGrossReceivedAmount();
        if (grossReceived.signum() > 0) return grossReceived;
        return receivable.getReceivedAmount();
    }

    private boolean hasReceivedAmount(AccountsReceivable receivable) {
        return receivedAmount(receivable).signum() > 0;
    }

    private String paymentInstallmentsText(Quote quote) {
        if (quote == null || quote.getInstallments() == null || quote.getInstallments() <= 1
                || !nonBlank(quote.getPaymentMethod())) return "";
        String methods = quote.getPaymentMethod().toLowerCase(Locale.ROOT);
        if (!methods.contains("crédito") && !methods.contains("credito") && !methods.contains("link de pagamento")) {
            return "";
        }
        String paymentType = methods.contains("link de pagamento")
                ? "via Link de Pagamento" : "no Cartão de Crédito";
        return "Parcelado em " + quote.getInstallments() + "x " + paymentType;
    }

    private String value(String value) { return value == null ? "" : value; }
    private boolean nonBlank(String value) { return value != null && !value.isBlank(); }

    private String installationDatesText(WorkOrder order) {
        TreeSet<LocalDate> dates = new TreeSet<>();
        scheduleRepository.findByWorkOrderId(order.getId()).ifPresent(schedule -> {
            if (schedule.getScheduledDate() != null) dates.add(schedule.getScheduledDate());
            if (schedule.getOccurrences() != null) {
                schedule.getOccurrences().stream()
                        .map(occurrence -> occurrence.getOccurrenceDate())
                        .filter(date -> date != null)
                        .forEach(dates::add);
            }
        });

        if (dates.isEmpty() && order.getInstallDate() != null) dates.add(order.getInstallDate());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dates.stream().map(formatter::format).collect(Collectors.joining(", "));
    }
}
