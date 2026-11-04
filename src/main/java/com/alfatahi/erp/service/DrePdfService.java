package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.DreReport;
import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.repository.ProfileRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DrePdfService {

    public record Pdf(byte[] bytes, String fileName) { }
    public record Detail(String label, List<String> values, String total) { }
    public record Line(String label, String hint, String kind, boolean alert, List<String> values,
                       String total, String av, String variation, List<Detail> details) { }
    public record Table(String title, List<String> months, List<Line> lines) { }
    public record Indicator(String label, String value, String note) { }

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TemplateEngine templateEngine;
    private final ProfileRepository profileRepository;
    private final CompanyImageService companyImageService;

    public DrePdfService(TemplateEngine templateEngine, ProfileRepository profileRepository,
                         CompanyImageService companyImageService) {
        this.templateEngine = templateEngine;
        this.profileRepository = profileRepository;
        this.companyImageService = companyImageService;
    }

    @Transactional(readOnly = true)
    public Pdf render(DreReport report) throws Exception {
        Profile company = companyProfile();
        YearMonth first = report.months().getFirst();
        YearMonth last = report.months().getLast();
        boolean annual = "year".equals(report.view());
        String period = FinancialPeriod.monthly(annual ? first : last).from().format(DATE)
                + " a " + FinancialPeriod.monthly(last).to().format(DATE);

        List<Table> tables = new ArrayList<>();
        int columnsPerTable = annual ? 6 : report.months().size();
        for (int start = 0; start < report.months().size(); start += columnsPerTable) {
            int end = Math.min(start + columnsPerTable, report.months().size());
            List<Line> lines = new ArrayList<>();
            for (DreReport.Line line : report.lines()) {
                List<Detail> details = new ArrayList<>();
                for (DreReport.Detail detail : line.details()) {
                    details.add(new Detail(text(detail.label()), amounts(detail.values(), start, end), money(detail.total())));
                }
                BigDecimal av = report.showTotal() ? line.avTotal() : line.av()[line.av().length - 1];
                lines.add(new Line(text(line.label()), text(line.hint()), line.kind().name(), line.alert(),
                        amounts(line.values(), start, end), money(line.total()), percent(av),
                        line.variation() == null ? "-" : percent(line.variation()), details));
            }
            tables.add(new Table(annual ? (start == 0 ? "Janeiro a junho" : "Julho a dezembro") : "",
                    report.monthLabels().subList(start, end), lines));
        }

        Context context = new Context(PT_BR);
        context.setVariable("companyName", text(company.getCompanyName()));
        context.setVariable("companyDocument", text(company.getDocument()));
        context.setVariable("companyAddress", companyAddress(company));
        context.setVariable("companyPhone", text(company.getPhone()));
        context.setVariable("companyEmail", text(company.getEmail()));
        context.setVariable("logoBase64", companyImageService.logoDataUri(company));
        context.setVariable("focusLabel", text(report.focusLabel()));
        context.setVariable("period", period);
        context.setVariable("generatedDate", FinancialPeriod.today().format(DATE));
        context.setVariable("annual", annual);
        context.setVariable("showTotal", report.showTotal());
        context.setVariable("showVariation", report.showVariation());
        context.setVariable("tables", tables);
        context.setVariable("indicators", indicators(report.kpis()));
        context.setVariable("unclassifiedCount", report.unclassifiedCount());
        context.setVariable("unclassifiedTotal", money(report.unclassifiedTotal()));

        String html = templateEngine.process("dre-pdf", context);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerFonts(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(output);
        builder.run();
        return new Pdf(output.toByteArray(), "DRE - " + (annual ? last.getYear() : last.toString()) + ".pdf");
    }

    private Profile companyProfile() {
        List<Profile> profiles = profileRepository.findAll(Sort.by("companyName", "id"));
        return profiles.stream()
                .filter(profile -> companyImageService.companyKey(profile).filter("grupoglass"::equals).isPresent())
                .findFirst().orElseGet(() -> {
                    if (!profiles.isEmpty()) return profiles.getFirst();
                    Profile fallback = new Profile();
                    fallback.setCompanyName("Grupo Glass");
                    return fallback;
                });
    }

    private List<Indicator> indicators(DreReport.Kpis kpis) {
        return List.of(
                new Indicator("Receita bruta", money(kpis.grossRevenue()), ""),
                new Indicator("Receita líquida", money(kpis.netRevenue()), ""),
                new Indicator("Lucro bruto", money(kpis.grossProfit()), "Margem: " + percent(kpis.grossMarginPct())),
                new Indicator("Resultado operacional", money(kpis.operatingResult()), "Margem: " + percent(kpis.operatingMarginPct())),
                new Indicator("Lucro líquido", money(kpis.netProfit()), "Margem: " + percent(kpis.netMarginPct())),
                new Indicator("Margem de contribuição", money(kpis.contributionMargin()), percent(kpis.contributionMarginPct()) + " da receita"),
                new Indicator("Custos e despesas fixos", money(kpis.fixedCosts()), ""),
                new Indicator("Ponto de equilíbrio", kpis.breakEvenRevenue() == null ? "Não aplicável" : money(kpis.breakEvenRevenue()), ""),
                new Indicator("Perdas registradas", money(kpis.losses()), percent(kpis.lossesPct()) + " da receita; informativo"),
                new Indicator("Saídas fora da DRE", money(kpis.outsideResult()), "Não alteram o lucro líquido"),
                new Indicator("Resultado após destinações", money(kpis.afterAllocations()), "Após investimentos, dívidas, reservas e retiradas"));
    }

    private List<String> amounts(BigDecimal[] values, int start, int end) {
        List<String> result = new ArrayList<>();
        for (int i = start; i < end; i++) result.add(money(values[i]));
        return result;
    }

    private static String companyAddress(Profile company) {
        return text(company.getAddress()) + (company.getCity() == null || company.getCity().isBlank()
                ? "" : (company.getAddress() == null || company.getAddress().isBlank() ? "" : " - ") + text(company.getCity()));
    }

    private static String text(String value) {
        return value == null ? "" : value.replace('\u2212', '-').replace('\u2013', '-')
                .replace('\u2014', '-').replace('\u2011', '-').replace("›", " / ");
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    private static String percent(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        return format.format(value == null ? BigDecimal.ZERO : value) + "%";
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
}
