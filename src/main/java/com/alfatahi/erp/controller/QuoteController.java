package com.alfatahi.erp.controller;

import com.alfatahi.erp.service.FinancialPeriod;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.entity.QuoteItem;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.QuoteRepository;
import com.alfatahi.erp.service.CompanyImageService;
import com.alfatahi.erp.service.QuoteService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/quotes")
public class QuoteController {

    private static final Logger log = LoggerFactory.getLogger(QuoteController.class);

    private final QuoteRepository quoteRepo;
    private final ClientRepository clientRepo;
    private final QuoteService quoteService;
    private final com.alfatahi.erp.service.ScheduleService scheduleService;
    private final com.alfatahi.erp.repository.ProfileRepository profileRepo;
    private final TemplateEngine templateEngine;
    private final CompanyImageService companyImageService;

    public QuoteController(QuoteRepository quoteRepo, ClientRepository clientRepo, QuoteService quoteService,
                           com.alfatahi.erp.service.ScheduleService scheduleService,
                           com.alfatahi.erp.repository.ProfileRepository profileRepo,
                           TemplateEngine templateEngine,
                           CompanyImageService companyImageService) {
        this.quoteRepo = quoteRepo;
        this.clientRepo = clientRepo;
        this.quoteService = quoteService;
        this.scheduleService = scheduleService;
        this.profileRepo = profileRepo;
        this.templateEngine = templateEngine;
        this.companyImageService = companyImageService;
    }

    @GetMapping("/pdf/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        try {
            Quote quote = quoteRepo.findById(id).orElseThrow();
            Hibernate.initialize(quote.getItems());
            if (quote.getProfile() != null) Hibernate.initialize(quote.getProfile());
            if (quote.getClient() != null)  Hibernate.initialize(quote.getClient());

            List<Map<String, String>> itemRows = new ArrayList<>();
            BigDecimal gross = BigDecimal.ZERO;

            for (QuoteItem item : quote.getItems()) {
                BigDecimal w   = nvl(item.getWidth());
                BigDecimal h   = nvl(item.getHeight());
                BigDecimal qty = nvl(item.getQuantity(), BigDecimal.ONE);
                BigDecimal up  = nvl(item.getUnitPrice());

                BigDecimal m2        = (w.compareTo(BigDecimal.ZERO) > 0 && h.compareTo(BigDecimal.ZERO) > 0) ? w.multiply(h) : BigDecimal.ONE;
                BigDecimal calcUnit  = m2.multiply(up);
                BigDecimal subtotal  = qty.multiply(calcUnit);
                gross = gross.add(subtotal);

                Map<String, String> row = new LinkedHashMap<>();
                row.put("category",  nvlStr(item.getCategory(), "Item"));
                row.put("product",   nvlStr(item.getProduct(),  ""));
                row.put("quantity",  qty.stripTrailingZeros().toPlainString());
                row.put("unitPrice", formatBRL(calcUnit));
                row.put("subtotal",  formatBRL(subtotal));
                itemRows.add(row);
            }

            BigDecimal discountPct = nvl(quote.getDiscountPercent());
            boolean hasDiscount    = discountPct.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal net;
            if (quote.getTotalValue() != null && quote.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
                net = quote.getTotalValue();
            } else {
                BigDecimal discAmt = gross.multiply(discountPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                net = gross.subtract(discAmt);
            }

            String pm = nvlStr(quote.getPaymentMethod(), "");
            boolean showInstall = (pm.contains("Crédito") || pm.contains("Link de Pagamento"))
                    && quote.getInstallments() != null && quote.getInstallments() > 1;
            String installText = "";
            if (showInstall) {
                BigDecimal val = net.divide(new BigDecimal(quote.getInstallments()), 2, RoundingMode.HALF_UP);
                installText = "Em até " + quote.getInstallments() + "x de " + formatBRL(val);
            }

            String companyName    = quote.getProfile() != null ? nvlStr(quote.getProfile().getCompanyName(), "") : "";
            String companyDoc     = quote.getProfile() != null ? nvlStr(quote.getProfile().getDocument(), "--") : "--";
            String companyAddress = quote.getProfile() != null ? nvlStr(quote.getProfile().getAddress(), "") : "";
            String companyEmail   = quote.getProfile() != null ? nvlStr(quote.getProfile().getEmail(), "") : "";
            String companyPhone   = quote.getProfile() != null ? nvlStr(quote.getProfile().getPhone(), "") : "";

            String clientName = quote.getClient() != null ? nvlStr(quote.getClient().getName(), "Consumidor Final") : "Consumidor Final";
            String clientDoc  = quote.getClient() != null ? nvlStr(quote.getClient().getDocument(), "") : "";
            String clientPhone= quote.getClient() != null ? nvlStr(quote.getClient().getPhone(), "") : "";
            String clientEmail= quote.getClient() != null ? nvlStr(quote.getClient().getEmail(), "") : "";
            String clientAddr = "";
            if (quote.getClient() != null) {
                String addr = nvlStr(quote.getClient().getAddress(), "");
                String city = nvlStr(quote.getClient().getCity(), "");
                clientAddr = addr + (!addr.isEmpty() && !city.isEmpty() ? " - " : "") + city;
            }
            boolean hasClientDetails = !clientDoc.isEmpty() || !clientPhone.isEmpty() || !clientEmail.isEmpty() || !clientAddr.isEmpty();

            String logoB64   = companyImageService.logoDataUri(quote.getProfile());
            String sigCoB64  = companyImageService.signatureDataUri(quote.getProfile());
            String sigCliB64 = clientSignatureDataUri(quote.getClientSignature());
            boolean clientSigned = quote.getClientSignature() != null && !quote.getClientSignature().isBlank();

            String num = nvlStr(quote.getNumber(), "ORC-0000").replace("ORC-", "");
            String dt  = quote.getDateCreated() != null
                    ? quote.getDateCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "--/--/----";

            String seller = nvlStr(quote.getSellerName(), "Não informado");

            boolean hasValidUntil = quote.getValidUntil() != null;
            String validUntilFormatted = hasValidUntil
                    ? quote.getValidUntil().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";

            Context ctx = new Context(Locale.forLanguageTag("pt-BR"));
            ctx.setVariable("sellerName",        seller);
            ctx.setVariable("numDisplay",        num);
            ctx.setVariable("dateFormatted",     dt);
            ctx.setVariable("hasValidUntil",     hasValidUntil);
            ctx.setVariable("validUntilFormatted", validUntilFormatted);
            ctx.setVariable("companyName",       companyName);
            ctx.setVariable("companyDoc",        companyDoc);
            ctx.setVariable("companyAddress",    companyAddress);
            ctx.setVariable("companyEmail",      companyEmail);
            ctx.setVariable("companyPhone",      companyPhone);
            ctx.setVariable("clientName",        clientName);
            ctx.setVariable("clientDoc",         clientDoc);
            ctx.setVariable("clientPhone",       clientPhone);
            ctx.setVariable("clientEmail",       clientEmail);
            ctx.setVariable("clientAddress",     clientAddr);
            ctx.setVariable("hasClientDetails",  hasClientDetails);
            ctx.setVariable("itemRows",          itemRows);
            ctx.setVariable("hasDiscount",       hasDiscount);
            ctx.setVariable("gross",             formatBRL(gross));
            ctx.setVariable("discountPct",       discountPct.stripTrailingZeros().toPlainString());
            ctx.setVariable("net",               formatBRL(net));
            ctx.setVariable("showInstallments",  showInstall);
            ctx.setVariable("installmentsText",  installText);
            ctx.setVariable("paymentMethod",     nvlStr(quote.getPaymentMethod(), "Não informado"));
            ctx.setVariable("warranty",          nvlStr(quote.getWarranty(), ""));
            ctx.setVariable("observations",      nvlStr(quote.getObservations(), ""));
            ctx.setVariable("clientSigned",      clientSigned);
            ctx.setVariable("logoBase64",        logoB64);
            ctx.setVariable("sigCompanyBase64",  sigCoB64);
            ctx.setVariable("sigClientBase64",   sigCliB64);

            String html = templateEngine.process("quote-pdf", ctx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            byte[] pdfBytes = out.toByteArray();

            String rawName = nvlStr(quote.getNumber(), "ORC-0000")
                    + " - " + clientName
                    + " - " + (companyName.isEmpty() ? "Empresa" : companyName);
            String fileName = rawName.replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ").trim() + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName.replace("\"", "'") + "\"")
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Erro ao gerar PDF do orçamento {}", id, e);
            String message = "Erro ao gerar PDF: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(message.getBytes(StandardCharsets.UTF_8));
        }
    }



    private void registerFonts(PdfRendererBuilder builder) {
        for (int weight : new int[]{400, 500, 600, 700, 800, 900}) {
            String path = "/fonts/Inter-" + weight + ".ttf";
            if (getClass().getResource(path) != null) {
                builder.useFont(() -> getClass().getResourceAsStream(path), "Inter", weight,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }
        }
    }
    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private BigDecimal nvl(BigDecimal v, BigDecimal def) { return v != null ? v : def; }
    private String nvlStr(String v, String def) { return (v != null && !v.isBlank()) ? v : (def != null ? def : ""); }

    private String formatBRL(BigDecimal v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return nf.format(v != null ? v : BigDecimal.ZERO);
    }

    private String clientSignatureDataUri(String signature) {
        if (signature == null || signature.isBlank()) return null;
        String trimmed = signature.trim();
        return trimmed.startsWith("data:image/") ? trimmed : null;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public String index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String name,
            Model model) {


        List<Quote> todosOrcamentos = quoteRepo.findAll();

        final boolean ordenarPorAprovacao = "approved".equals(status);

        todosOrcamentos.sort((q1, q2) -> {
            if (ordenarPorAprovacao) {
                LocalDateTime aprov1 = q1.getDateApproved();
                LocalDateTime aprov2 = q2.getDateApproved();
                if (aprov1 == null && aprov2 == null) return 0;
                if (aprov1 == null) return 1;
                if (aprov2 == null) return -1;
                return aprov2.compareTo(aprov1);
            }
            if (q1.getDateCreated() == null || q2.getDateCreated() == null) return 0;
            int dataCompare = q2.getDateCreated().compareTo(q1.getDateCreated());
            if (dataCompare != 0) return dataCompare;
            if (q1.getNumber() == null) return 1;
            if (q2.getNumber() == null) return -1;
            return q2.getNumber().compareTo(q1.getNumber());
        });

        FinancialPeriod period = FinancialPeriod.select(month, false, null, null);
        period.addTo(model);
        month = period.reference() == null ? "all" : period.monthValue();
        List<Quote> filteredList = todosOrcamentos.stream().filter(q -> {
            if (status != null && !status.isEmpty() && !q.getStatus().equals(status)) return false;
            if (number != null && !number.isEmpty() && !q.getNumber().toLowerCase().contains(number.toLowerCase())) return false;
            if (name != null && !name.isEmpty() && q.getClient() != null && !q.getClient().getName().toLowerCase().contains(name.toLowerCase())) return false;

            if (!period.contains(ordenarPorAprovacao ? q.getDateApproved() : q.getDateCreated())) return false;
            return true;
        }).collect(java.util.stream.Collectors.toList());

        List<Map<String, String>> disponiveis = new ArrayList<>();

        java.util.SortedSet<java.time.YearMonth> references = new java.util.TreeSet<>(java.util.Comparator.reverseOrder());
        references.add(FinancialPeriod.referenceFor(FinancialPeriod.today()));
        if (period.reference() != null) references.add(period.reference());
        for (Quote quote : todosOrcamentos) {
            if (quote.getDateCreated() != null) references.add(FinancialPeriod.referenceFor(quote.getDateCreated().toLocalDate()));
            if (quote.getDateApproved() != null) references.add(FinancialPeriod.referenceFor(quote.getDateApproved().toLocalDate()));
        }
        for (java.time.YearMonth reference : references) {
            disponiveis.add(Map.of("value", reference.toString(), "label", FinancialPeriod.monthly(reference).label()));
        }

        List<com.alfatahi.erp.entity.Profile> profiles = profileRepo.findAll();

        model.addAttribute("quote", new Quote());
        model.addAttribute("currentPage", "quotes");
        model.addAttribute("quotes", filteredList);
        model.addAttribute("clients", clientRepo.findAll());
        model.addAttribute("profiles", profiles);
        model.addAttribute("availableMonths", disponiveis);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedNumber", number);
        model.addAttribute("selectedName", name);
        model.addAttribute("selectedFilterStatus", status);

        return "quotes";
    }

    @GetMapping("/new")
    public String newQuoteForm(Model model) {
        model.addAttribute("currentPage", "quotes");
        model.addAttribute("clients", clientRepo.findAll());
        model.addAttribute("profiles", profileRepo.findAll());
        model.addAttribute("quoteId", "");
        return "quote-form";
    }

    @GetMapping("/{id}/edit")
    @Transactional(readOnly = true)
    public String editQuoteForm(@PathVariable UUID id, Model model) {

        Quote quote = quoteRepo.findById(id).orElseThrow();

        model.addAttribute("currentPage", "quotes");
        model.addAttribute("clients", clientRepo.findAll());
        model.addAttribute("profiles", profileRepo.findAll());
        model.addAttribute("quoteId", id.toString());
        model.addAttribute("quoteStatus", quote.getStatus());
        return "quote-form";
    }

    @GetMapping("/view-data/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Quote> getQuoteData(@PathVariable UUID id) {
        Quote quote = quoteRepo.findById(id).orElseThrow();

        Hibernate.initialize(quote.getItems());

        if (quote.getWorkOrder() != null) {
            Hibernate.initialize(quote.getWorkOrder().getItems());
        }

        if (quote.getProfile() != null) {
            Hibernate.initialize(quote.getProfile());
        }

        return ResponseEntity.ok(quote);
    }

    private LocalDate resolveValidUntil(LocalDate informed, LocalDateTime issuedAt, LocalDate current) {
        if (informed != null) return informed;
        if (current != null) return current;
        LocalDateTime base = issuedAt != null ? issuedAt : LocalDateTime.now();
        return base.toLocalDate().plusDays(QuoteService.DEFAULT_VALIDITY_DAYS);
    }

    private void ensureItemDescriptions(List<QuoteItem> items) {
        if (items == null) return;
        for (QuoteItem item : items) {
            if (item.getDescription() == null || item.getDescription().isBlank()) {
                String cat = (item.getCategory() != null && !item.getCategory().isBlank()) ? item.getCategory() : "Item";
                String prod = item.getProduct() != null ? item.getProduct() : "";
                item.setDescription(prod.isBlank() ? cat : cat + " - " + prod);
            }
        }
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> saveAjax(@RequestBody Quote quote, java.security.Principal principal) {

        ensureItemDescriptions(quote.getItems());

        if (quote.getId() != null) {
            Quote existing = quoteRepo.findById(quote.getId()).orElseThrow();
            existing.setClient(quote.getClient());
            existing.setProfile(quote.getProfile());
            if (quote.getDateCreated() != null) {
                existing.setDateCreated(quote.getDateCreated());
            }
            existing.setPaymentMethod(quote.getPaymentMethod());
            existing.setInstallments(quote.getInstallments());
            existing.setObservations(quote.getObservations());
            existing.setWarranty(quote.getWarranty());
            existing.setTotalValue(quote.getTotalValue());
            existing.setDiscountPercent(quote.getDiscountPercent());
            existing.setValidUntil(resolveValidUntil(quote.getValidUntil(), existing.getDateCreated(), existing.getValidUntil()));

            existing.getItems().clear();
            if (quote.getItems() != null) {
                for (QuoteItem item : quote.getItems()) {
                    item.setQuote(existing);
                    existing.getItems().add(item);
                }
            }

            quoteRepo.save(existing);

            if ("approved".equals(existing.getStatus())) {
                quoteService.syncApprovedQuoteToWorkOrder(existing);
            }

            return ResponseEntity.ok().build();
        }

        if (quote.getNumber() == null || quote.getNumber().isEmpty()) {
            int next = quoteRepo.findMaxQuoteSequence() + 1;
            quote.setNumber(String.format("ORC-%04d", next));
        }

        if (quote.getDateCreated() == null) {
            quote.setDateCreated(LocalDateTime.now());
        }

        quote.setValidUntil(resolveValidUntil(quote.getValidUntil(), quote.getDateCreated(), null));

        if (principal != null) {
            quote.setSellerName(principal.getName());
        }

        quote.setStatus("pending");
        quote.setDateApproved(null);
        quote.setWorkOrder(null);

        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                item.setQuote(quote);
            }
        }
        quoteRepo.save(quote);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approve/{id}")
    @ResponseBody
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        try {
            quoteService.approveQuote(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao aprovar: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        quoteService.deleteQuote(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-signature")
    @ResponseBody
    public ResponseEntity<?> resetSignature(@PathVariable UUID id) {
        try {
            quoteService.resetClientSignature(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao reverter assinatura: " + e.getMessage());
        }
    }

    @PostMapping(value = "/add-client-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<com.alfatahi.erp.entity.Client> addClientAjax(@RequestBody com.alfatahi.erp.entity.Client client) {
        client.setIsActive(true);
        com.alfatahi.erp.entity.Client savedClient = clientRepo.save(client);
        return ResponseEntity.ok(savedClient);
    }
}