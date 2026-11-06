package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.FinancialResultReport;
import com.alfatahi.erp.dto.FinancialResultReport.*;
import com.alfatahi.erp.dto.FinancialResultFilter;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class FinancialResultPdfService {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Pdf(byte[] bytes, String fileName) { }
    public record Comparison(String label, String previous, String current, String difference) { }
    public record CostRow(String description, String category, String dueDate, String paymentDate,
                          String status, String amount, String paid, String pending,
                          String included, String source, String note) { }
    public record OrderRow(String number, String client, String description, String saleDate, String status,
                           String sale, String materials, String labor, String otherCosts, String totalCosts,
                           String profit, String margin, String received, String receivedPeriod, String receivedBefore,
                           String receivable, String paid, String paidPeriod, String paidBefore, String payable,
                           String cashResult, String directPaidPeriod, String lastMovement,
                           List<String> situations, List<String> warnings, List<CostRow> costs) { }
    public record MovementRow(String date, String description, String category, String orders, String party,
                              String type, String incoming, String outgoing, String paymentMethod,
                              String allocation, String reconciliation) { }
    public record CrossOrderRow(String number, String client, String saleDate, List<String> situations,
                                String sale, String received, String receivable, String totalCosts,
                                String paid, String payable, String profit, String lastMovement) { }
    public record OperatingRow(String label, String amount, boolean result, boolean outside) { }
    public record Data(String companyName, String companyDocument, String companyAddress, String companyPhone,
                       String companyEmail, String logoBase64, String period, String generatedDate,
                       String filterSummary, String previousPeriod, String cashOpening, String cashIncoming,
                       String cashOutgoing, String cashNet, String cashClosing, String cashCount,
                       String previousCount, String previousIncoming, String previousOutgoing, String previousNet,
                       List<Comparison> comparisons, List<OrderRow> orders, List<MovementRow> movements,
                       String oldSaleReceipts, String currentSaleReceivables, String oldSalePayments,
                       String pendingCostsInProgress, List<CrossOrderRow> crossOrders,
                       List<OperatingRow> operating, List<String> warnings) { }

    private final TemplateEngine templateEngine;
    private final ProfileRepository profileRepository;
    private final CompanyImageService companyImageService;

    public FinancialResultPdfService(TemplateEngine templateEngine, ProfileRepository profileRepository,
                                     CompanyImageService companyImageService) {
        this.templateEngine = templateEngine;
        this.profileRepository = profileRepository;
        this.companyImageService = companyImageService;
    }

    @Transactional(readOnly = true)
    public Pdf render(FinancialResultReport report) throws Exception {
        Profile company = companyProfile();
        Data data = data(report, company);
        Context context = new Context(PT_BR);
        context.setVariable("data", data);
        String html = templateEngine.process("financial-result-pdf", context);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        registerFonts(builder);
        builder.withHtmlContent(html, null);
        builder.toStream(output);
        builder.run();
        String fileName = "Resultado Financeiro - " + report.filter().from().format(DATE).replace('/', '-')
                + " a " + report.filter().to().format(DATE).replace('/', '-') + ".pdf";
        return new Pdf(output.toByteArray(), fileName);
    }

    private Data data(FinancialResultReport report, Profile company) {
        FinancialResultFilter filter = report.filter();
        CashSummary cash = report.cash();
        CashSummary previous = report.previous();
        String previousPeriod = date(filter.previous().from()) + " a " + date(filter.previous().to());
        List<Comparison> comparisons = List.of(
                new Comparison("Entradas", money(previous.incoming()), money(cash.incoming()), money(cash.incoming().subtract(previous.incoming()))),
                new Comparison("Saídas", money(previous.outgoing()), money(cash.outgoing()), money(cash.outgoing().subtract(previous.outgoing()))),
                new Comparison("Resultado de caixa", money(previous.net()), money(cash.net()), money(cash.net().subtract(previous.net()))));
        return new Data(text(company.getCompanyName()), text(company.getDocument()), companyAddress(company),
                text(company.getPhone()), text(company.getEmail()), companyImageService.logoDataUri(company),
                date(filter.from()) + " a " + date(filter.to()), date(FinancialPeriod.today()), filterSummary(filter, report.orders()),
                previousPeriod, money(cash.opening()), money(cash.incoming()), money(cash.outgoing()), money(cash.net()),
                money(cash.closing()), Long.toString(cash.count()), Long.toString(previous.count()), money(previous.incoming()), money(previous.outgoing()),
                money(previous.net()), comparisons, report.orders().stream().map(this::order).toList(),
                report.movements().stream().map(this::movement).toList(), money(report.crossPeriod().receiptsOldSales()),
                money(report.crossPeriod().receivablesNewSales()), money(report.crossPeriod().paymentsOldSales()),
                money(report.crossPeriod().pendingCostsInProgress()), report.crossPeriod().orders().stream().map(this::crossOrder).toList(),
                report.operating().stream().map(line -> new OperatingRow(text(line.label()), money(line.amount()), line.result(), line.outside())).toList(),
                report.warnings().stream().map(FinancialResultPdfService::text).toList());
    }

    private OrderRow order(FinancialResultReport.OrderResult order) {
        return new OrderRow(text(order.number()), text(order.client()), text(order.description()), date(order.saleDate()),
                order.statusLabel(), money(order.sale()), money(order.materials()), money(order.labor()), money(order.otherCosts()),
                money(order.totalCosts()), order.profit() == null ? "Não calculado" : money(order.profit()),
                percent(order.margin()), money(order.received()), money(order.receivedPeriod()), money(order.receivedBefore()),
                money(order.receivable()), money(order.paid()), money(order.paidPeriod()), money(order.paidBefore()),
                money(order.payable()), money(order.cashResult()), money(order.directPaidPeriod()), date(order.lastMovement()),
                order.situations().stream().map(FinancialResultPdfService::text).toList(),
                order.warnings().stream().map(FinancialResultPdfService::text).toList(),
                order.costDetails().stream().map(this::cost).toList());
    }

    private CostRow cost(CostDetail cost) {
        String note = cost.invalid() ? "Rateio inválido; revisar" : "";
        return new CostRow(text(cost.description()), text(cost.category()), date(cost.dueDate()), date(cost.paymentDate()),
                status(cost.status()), money(cost.amount()), money(cost.paid()), money(cost.pending()),
                cost.directCost() ? "Sim" : "Não", cost.manual() ? "Estimativa manual" : "Conta a pagar", note);
    }

    private MovementRow movement(Movement movement) {
        String orders = movement.orders().isEmpty() ? "Sem OS" : movement.orders().stream().map(Option::label).collect(Collectors.joining(", "));
        return new MovementRow(date(movement.date()), text(movement.description()), text(movement.category()), text(orders),
                text(movement.party()), "ENTRADA".equals(movement.type()) ? "Entrada" : "Saída", money(movement.incoming()),
                money(movement.outgoing()), text(movement.paymentMethod()).isBlank() ? "Não informada" : text(movement.paymentMethod()),
                movement.allocated() ? "Pagamento rateado" : "Não", text(movement.reconciliation()));
    }

    private CrossOrderRow crossOrder(FinancialResultReport.OrderResult order) {
        return new CrossOrderRow(text(order.number()), text(order.client()), date(order.saleDate()),
                order.situations().stream().map(FinancialResultPdfService::text).toList(), money(order.sale()),
                money(order.received()), money(order.receivable()), money(order.totalCosts()), money(order.paid()),
                money(order.payable()), order.profit() == null ? "Não calculado" : money(order.profit()), date(order.lastMovement()));
    }

    private static String filterSummary(FinancialResultFilter filter, List<FinancialResultReport.OrderResult> orders) {
        List<String> selected = new java.util.ArrayList<>();
        if (!filter.category().isBlank()) selected.add("Categoria: " + filter.category());
        if (filter.workOrderId() != null) {
            String orderLabel = orders.stream().filter(order -> order.id().equals(filter.workOrderId()))
                    .findFirst().map(order -> order.number() + " - " + order.client()).orElse(filter.workOrderId().toString());
            selected.add("OS selecionada: " + orderLabel);
        }
        if (!filter.type().isBlank()) selected.add("Movimento: " + ("ENTRADA".equals(filter.type()) ? "Entradas" : "Saídas"));
        if (!filter.receiptStatus().isBlank()) selected.add("Contas a receber: " + status(filter.receiptStatus()));
        if (!filter.paymentStatus().isBlank()) selected.add("Contas a pagar: " + status(filter.paymentStatus()));
        if (selected.isEmpty()) return "Visão geral do período, sem recortes adicionais.";
        return String.join(" · ", selected);
    }

    private Profile companyProfile() {
        List<Profile> profiles = profileRepository.findAll(Sort.by("companyName", "id"));
        return profiles.stream().filter(profile -> companyImageService.companyKey(profile).filter("grupoglass"::equals).isPresent())
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

    private static String status(String value) {
        if (value == null || value.isBlank()) return "Não informado";
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "pending" -> "Pendente";
            case "partial" -> "Parcial";
            case "paid", "received" -> "Paga / recebida";
            case "cancelled", "canceled", "inactive" -> "Cancelada / inativa";
            default -> text(value);
        };
    }

    private static String text(String value) {
        return value == null ? "" : value.replace('\u2212', '-').replace('\u2013', '-')
                .replace('\u2014', '-').replace('\u2011', '-').replace("›", " / ");
    }

    private static String date(LocalDate value) { return value == null ? "-" : value.format(DATE); }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value == null ? BigDecimal.ZERO : value);
    }

    private static String percent(BigDecimal value) {
        if (value == null) return "Não calculada";
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(value) + "%";
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
