package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.ReceiptRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReceiptPdfService {

    public record Pdf(byte[] bytes, String fileName) { }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReceiptRepository receiptRepo;
    private final TemplateEngine templateEngine;
    private final CompanyImageService companyImageService;

    public ReceiptPdfService(ReceiptRepository receiptRepo, TemplateEngine templateEngine,
                             CompanyImageService companyImageService) {
        this.receiptRepo = receiptRepo;
        this.templateEngine = templateEngine;
        this.companyImageService = companyImageService;
    }

    @Transactional(readOnly = true)
    public Pdf render(UUID id) throws Exception {
        Receipt r = receiptRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recibo não encontrado."));

        Profile profile = r.getProfile();
        Client client = r.getClient();

        List<Map<String, String>> itemRows = new ArrayList<>();
        BigDecimal gross = BigDecimal.ZERO;
        for (ReceiptItem item : r.getItems()) {
            Map<String, String> row = new LinkedHashMap<>();
            String[] parts = ReceiptService.splitDescription(item.getDescription());
            row.put("category", parts[0]);
            row.put("product", parts[1]);
            row.put("quantity", item.getQuantity().stripTrailingZeros().toPlainString());
            row.put("unitPrice", formatBRL(item.getUnitPrice()));
            row.put("subtotal", formatBRL(item.getSubtotal()));
            itemRows.add(row);
            gross = gross.add(item.getSubtotal());
        }

        List<String> photoUris = new ArrayList<>();
        for (ReceiptPhoto p : r.getPhotos()) {
            photoUris.add("data:" + p.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(p.getContent()));
        }

        String companyName = profile != null ? nvl(profile.getCompanyName()) : "";
        String companyDoc = profile != null && profile.getDocument() != null && !profile.getDocument().isBlank()
                ? profile.getDocument() : "--";
        String companyAddress = profile != null ? nvl(profile.getAddress()) : "";
        String companyEmail = profile != null ? nvl(profile.getEmail()) : "";
        String companyPhone = profile != null ? nvl(profile.getPhone()) : "";

        String clientName = client != null && client.getName() != null && !client.getName().isBlank()
                ? client.getName() : "Cliente";
        String clientDoc = client != null ? nvl(client.getDocument()) : "";
        String clientPhone = client != null ? nvl(client.getPhone()) : "";
        String clientEmail = client != null ? nvl(client.getEmail()) : "";
        String clientAddress = ReceiptService.fullAddress(client);
        boolean hasClientDetails = !clientDoc.isEmpty() || !clientPhone.isEmpty()
                || !clientEmail.isEmpty() || !clientAddress.isEmpty();

        BigDecimal discount = r.getDiscount();

        Context ctx = new Context(Locale.forLanguageTag("pt-BR"));
        ctx.setVariable("numDisplay", nvl(r.getNumber()).replace("REC-", ""));
        ctx.setVariable("issueDateFormatted", fmt(r.getIssueDate()));
        ctx.setVariable("receivedDateFormatted", fmt(r.getReceivedDate()));
        ctx.setVariable("workOrderNumber", r.getWorkOrder() != null ? nvl(r.getWorkOrder().getNumber()) : null);
        ctx.setVariable("sellerName", nvl(r.getCreatedByName()));
        ctx.setVariable("companyName", companyName);
        ctx.setVariable("companyDoc", companyDoc);
        ctx.setVariable("companyAddress", companyAddress);
        ctx.setVariable("companyEmail", companyEmail);
        ctx.setVariable("companyPhone", companyPhone);
        ctx.setVariable("clientName", clientName);
        ctx.setVariable("clientDoc", clientDoc);
        ctx.setVariable("clientPhone", clientPhone);
        ctx.setVariable("clientEmail", clientEmail);
        ctx.setVariable("clientAddress", clientAddress);
        ctx.setVariable("hasClientDetails", hasClientDetails);
        ctx.setVariable("itemRows", itemRows);
        ctx.setVariable("hasDiscount", discount.signum() > 0);
        ctx.setVariable("gross", formatBRL(gross));
        ctx.setVariable("discountValue", formatBRL(discount));
        ctx.setVariable("net", formatBRL(r.getTotalAmount()));
        ctx.setVariable("description", nvl(r.getDescription()));
        ctx.setVariable("paymentTerms", nvl(r.getPaymentTerms()));
        ctx.setVariable("warranty", nvl(r.getWarranty()));
        ctx.setVariable("photos", photoUris);
        ctx.setVariable("logoBase64", companyImageService.logoDataUri(profile));
        ctx.setVariable("sigCompanyBase64", companyImageService.signatureDataUri(profile));
        String clientSig = ReceiptService.signatureDataUri(r.getClientSignature());
        ctx.setVariable("sigClientBase64", clientSig);
        ctx.setVariable("clientSigned", clientSig != null);

        String html = templateEngine.process("receipt-pdf", ctx);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerFonts(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();

        String number = nvl(r.getNumber()).replace("REC-", "REC ");
        String rawName = number + " - " + clientName.toUpperCase(Locale.forLanguageTag("pt-BR"));
        String fileName = rawName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").replaceAll("\\s+", " ").trim() + ".pdf";
        return new Pdf(out.toByteArray(), fileName);
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

    private static String fmt(LocalDate d) { return d != null ? d.format(DATE_FMT) : "--/--/----"; }

    private static String nvl(String v) { return v != null ? v : ""; }

    private static String formatBRL(BigDecimal v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return nf.format(v != null ? v : BigDecimal.ZERO);
    }
}
