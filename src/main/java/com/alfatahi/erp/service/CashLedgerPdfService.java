package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.CashLedgerEntryDto;
import com.alfatahi.erp.dto.CashLedgerReport;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class CashLedgerPdfService {

    public record Pdf(byte[] bytes, String fileName) { }
    public record Line(String date, String type, String party, String description, String workOrderNumber,
                       String paymentMethod, String account, String entrada, String saida, String saldo,
                       String notes, boolean financialExpense, boolean negativeBalance) { }

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TemplateEngine templateEngine;
    private final ProfileRepository profileRepository;
    private final CompanyImageService companyImageService;

    public CashLedgerPdfService(TemplateEngine templateEngine, ProfileRepository profileRepository,
                                CompanyImageService companyImageService) {
        this.templateEngine = templateEngine;
        this.profileRepository = profileRepository;
        this.companyImageService = companyImageService;
    }

    @Transactional(readOnly = true)
    public Pdf render(CashLedgerReport report) throws Exception {
        Profile company = companyProfile();
        Context context = new Context(PT_BR);
        context.setVariable("companyName", text(company.getCompanyName()));
        context.setVariable("companyDocument", text(company.getDocument()));
        context.setVariable("companyAddress", companyAddress(company));
        context.setVariable("companyPhone", text(company.getPhone()));
        context.setVariable("companyEmail", text(company.getEmail()));
        context.setVariable("logoBase64", companyImageService.logoDataUri(company));
        context.setVariable("period", report.from().format(DATE) + " a " + report.to().format(DATE));
        context.setVariable("from", report.from().format(DATE));
        context.setVariable("to", report.to().format(DATE));
        context.setVariable("generatedDate", FinancialPeriod.today().format(DATE));
        context.setVariable("openingBalance", money(report.openingBalance()));
        context.setVariable("openingBankBalance", money(report.openingBalances().bank()));
        context.setVariable("openingCashBalance", money(report.openingBalances().cash()));
        context.setVariable("totalEntradas", money(report.totalEntradas()));
        context.setVariable("totalSaidas", money(report.totalSaidas()));
        context.setVariable("totalTaxas", money(report.totalTaxas()));
        context.setVariable("saldoFinal", money(report.saldoFinal()));
        context.setVariable("saldoBanco", money(report.closingBalances().bank()));
        context.setVariable("saldoDinheiro", money(report.closingBalances().cash()));
        context.setVariable("negativeBalance", report.saldoFinal().signum() < 0);
        context.setVariable("lines", report.entries().stream().map(this::line).toList());

        String html = templateEngine.process("cash-ledger-pdf", context);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerFonts(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(output);
        builder.run();
        return new Pdf(output.toByteArray(), "Fluxo de Caixa - " + report.from() + " a " + report.to() + ".pdf");
    }

    private Line line(CashLedgerEntryDto entry) {
        String type = entry.getType() == CashLedgerEntryDto.EntryType.ENTRADA ? "Entrada"
                : entry.isFinancialExpense() ? "Taxa" : "Saída";
        return new Line(entry.getDate().format(DATE), type, text(entry.getParty()), text(entry.getDescription()),
                text(entry.getWorkOrderNumber()), text(entry.getPaymentMethod()),
                entry.getBalanceLocation() == CashLedgerEntryDto.BalanceLocation.DINHEIRO ? "Dinheiro" : "Banco",
                entry.getEntrada().signum() == 0 ? "-" : money(entry.getEntrada()),
                entry.getSaida().signum() == 0 ? "-" : money(entry.getSaida()), money(entry.getSaldo()),
                text(entry.getNotes()), entry.isFinancialExpense(), entry.getSaldo().signum() < 0);
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
