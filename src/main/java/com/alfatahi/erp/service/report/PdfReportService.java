package com.alfatahi.erp.service.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renderiza qualquer template Thymeleaf de relatório (HTML) para um PDF (A4),
 * usando o mesmo motor (openhtmltopdf) já utilizado para o PDF de orçamento.
 */
@Service
public class PdfReportService {

    private final TemplateEngine templateEngine;

    public PdfReportService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] render(String templateName, Context context) {
        try {
            String html = templateEngine.process(templateName, context);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useSVGDrawer(new BatikSVGDrawer());
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PDF do relatório: " + e.getMessage(), e);
        }
    }

    // ── Helpers de formatação usados pelos controllers ao montar o Context ──

    public static String brl(BigDecimal v) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return nf.format(v != null ? v : BigDecimal.ZERO);
    }

    public static String pct(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).stripTrailingZeros().toPlainString() + "%";
    }

    public static String date(LocalDate d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
    }

    public static String dateTime(LocalDateTime d) {
        return d != null ? d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
    }
}
