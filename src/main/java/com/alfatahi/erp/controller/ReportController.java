package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.*;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.CashLedgerService;
import com.alfatahi.erp.service.ReportService;
import com.alfatahi.erp.service.report.ExcelReportService;
import com.alfatahi.erp.service.report.PdfReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.alfatahi.erp.service.report.PdfReportService.*;

/**
 * Central de Relatórios do ERP.
 *
 * Reúne, em um único módulo:
 *  - Relatórios de Ordem de Serviço (individual e consolidado), com item vendido,
 *    custo de material/produção, receita e lucro.
 *  - Relatórios de gestão (contas a receber, contas a pagar, fluxo de caixa, DRE,
 *    clientes e fornecedores).
 *  - Relatório voltado ao contador (DRE + despesas por categoria + movimentações do período).
 *
 * Todos os relatórios podem ser exportados em PDF ou Excel (.xlsx), com filtros
 * de período/cliente/status, tanto de forma consolidada quanto individual.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    private final ClientRepository clientRepository;
    private final SupplierRepository supplierRepository;
    private final AccountsPayableRepository payableRepo;
    private final AccountsReceivableRepository receivableRepo;
    private final ProfileRepository profileRepository;
    private final CashLedgerService cashLedgerService;

    public ReportController(ReportService reportService, ExcelReportService excelReportService,
                             PdfReportService pdfReportService, ClientRepository clientRepository,
                             SupplierRepository supplierRepository, AccountsPayableRepository payableRepo,
                             AccountsReceivableRepository receivableRepo, ProfileRepository profileRepository,
                             CashLedgerService cashLedgerService) {
        this.reportService = reportService;
        this.excelReportService = excelReportService;
        this.pdfReportService = pdfReportService;
        this.clientRepository = clientRepository;
        this.supplierRepository = supplierRepository;
        this.payableRepo = payableRepo;
        this.receivableRepo = receivableRepo;
        this.profileRepository = profileRepository;
        this.cashLedgerService = cashLedgerService;
    }

    // ══════════════════════════════════════════════════════════════════
    // HUB
    // ══════════════════════════════════════════════════════════════════

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        model.addAttribute("currentPage", "reports");
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("workOrders", reportService.filterWorkOrders(null, null, null, null));
        model.addAttribute("defaultFrom", LocalDate.now().withDayOfMonth(1));
        model.addAttribute("defaultTo", LocalDate.now());
        model.addAttribute("currentMonth", LocalDate.now().getMonthValue());
        model.addAttribute("currentYear", LocalDate.now().getYear());
        List<Integer> years = new ArrayList<>();
        int y = LocalDate.now().getYear();
        for (int i = 0; i < 5; i++) years.add(y - i);
        model.addAttribute("years", years);
        return "reports";
    }

    // ══════════════════════════════════════════════════════════════════
    // ORDENS DE SERVIÇO — INDIVIDUAL
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/work-orders/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> workOrderPdf(@PathVariable UUID id) {
        WorkOrderReportDto os = reportService.getWorkOrderReport(id);
        Profile profile = firstProfile();

        Context ctx = new Context(new Locale("pt", "BR"));
        ctx.setVariable("os", os);
        ctx.setVariable("companyName", nvl(profile.getCompanyName()));
        ctx.setVariable("companyDoc", nvl(profile.getDocument()));
        ctx.setVariable("companyAddress", nvl(profile.getAddress()));
        ctx.setVariable("companyEmail", nvl(profile.getEmail()));
        ctx.setVariable("companyPhone", nvl(profile.getPhone()));
        ctx.setVariable("createdAtFmt", dateTime(os.getCreatedAt()));
        ctx.setVariable("installDateFmt", os.getInstallDate() != null ? date(os.getInstallDate()) : null);
        ctx.setVariable("areaFmt", os.getArea() != null && os.getArea().compareTo(BigDecimal.ZERO) > 0
                ? os.getArea().setScale(2, RoundingMode.HALF_UP).toPlainString() : null);
        ctx.setVariable("revenueFmt", brl(os.getTotalRevenue()));
        ctx.setVariable("costFmt", brl(os.getTotalCost()));
        ctx.setVariable("profitFmt", brl(os.getProfit()));
        ctx.setVariable("profitPositive", os.getProfit().compareTo(BigDecimal.ZERO) >= 0);
        ctx.setVariable("marginFmt", pct(os.getMarginPercent()));
        ctx.setVariable("notes", null);
        ctx.setVariable("notesHtml", "");

        List<Map<String, Object>> itemRows = new ArrayList<>();
        for (WorkOrderItemReportDto item : os.getItems()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("description", item.getDescription());
            row.put("quantity", item.getQuantity().stripTrailingZeros().toPlainString());
            row.put("unitCost", brl(item.getUnitCost()));
            row.put("unitPrice", brl(item.getUnitPrice()));
            row.put("totalCost", brl(item.getTotalCost()));
            row.put("totalPrice", brl(item.getTotalPrice()));
            row.put("profit", brl(item.getProfit()));
            row.put("profitPositive", item.getProfit().compareTo(BigDecimal.ZERO) >= 0);
            itemRows.add(row);
        }
        ctx.setVariable("itemRows", itemRows);

        byte[] pdf = pdfReportService.render("os-individual-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_" + os.getNumber() + ".pdf");
    }

    @GetMapping("/work-orders/{id}/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> workOrderExcel(@PathVariable UUID id) {
        WorkOrderReportDto os = reportService.getWorkOrderReport(id);
        byte[] xls = excelReportService.workOrderIndividual(os);
        return excelResponse(xls, "Relatorio_" + os.getNumber() + ".xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // ORDENS DE SERVIÇO — CONSOLIDADO (todas ou filtradas)
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/work-orders/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> workOrdersConsolidatedPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean detailed) {

        List<WorkOrderReportDto> list = reportService.filterWorkOrders(from, to, clientId, status);
        WorkOrderReportTotalsDto totals = new WorkOrderReportTotalsDto();
        list.forEach(totals::accumulate);
        Profile profile = firstProfile();

        Context ctx = new Context(new Locale("pt", "BR"));
        ctx.setVariable("companyName", nvl(profile.getCompanyName()));
        ctx.setVariable("companyDoc", nvl(profile.getDocument()));
        ctx.setVariable("companyAddress", nvl(profile.getAddress()));
        ctx.setVariable("subtitle", periodSubtitle(from, to, clientId, status));
        ctx.setVariable("totalCount", totals.getQuantity());
        ctx.setVariable("revenueFmt", brl(totals.getTotalRevenue()));
        ctx.setVariable("costFmt", brl(totals.getTotalCost()));
        ctx.setVariable("profitFmt", brl(totals.getTotalProfit()));
        ctx.setVariable("marginFmt", pct(totals.getAverageMargin()));
        ctx.setVariable("detailed", detailed);

        List<Map<String, Object>> rows = list.stream().map(this::toPdfRow).collect(Collectors.toList());
        ctx.setVariable("rows", rows);

        if (detailed) {
            List<Map<String, Object>> detailRows = new ArrayList<>();
            for (WorkOrderReportDto os : list) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("number", os.getNumber());
                row.put("clientName", os.getClientName());
                row.put("statusLabel", os.getStatusLabel());
                List<Map<String, Object>> items = new ArrayList<>();
                for (WorkOrderItemReportDto item : os.getItems()) {
                    Map<String, Object> ir = new LinkedHashMap<>();
                    ir.put("description", item.getDescription());
                    ir.put("quantity", item.getQuantity().stripTrailingZeros().toPlainString());
                    ir.put("unitCost", brl(item.getUnitCost()));
                    ir.put("unitPrice", brl(item.getUnitPrice()));
                    ir.put("totalCost", brl(item.getTotalCost()));
                    ir.put("totalPrice", brl(item.getTotalPrice()));
                    items.add(ir);
                }
                row.put("items", items);
                detailRows.add(row);
            }
            ctx.setVariable("detailRows", detailRows);
        }

        byte[] pdf = pdfReportService.render("os-consolidated-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Consolidado_OS.pdf");
    }

    @GetMapping("/work-orders/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> workOrdersConsolidatedExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "false") boolean detailed) {

        List<WorkOrderReportDto> list = reportService.filterWorkOrders(from, to, clientId, status);
        WorkOrderReportTotalsDto totals = new WorkOrderReportTotalsDto();
        list.forEach(totals::accumulate);

        byte[] xls = excelReportService.workOrdersConsolidated(list, totals, detailed, periodSubtitle(from, to, clientId, status));
        return excelResponse(xls, "Relatorio_Consolidado_OS.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // CONTAS A RECEBER
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/receivables/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> receivablesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {

        List<AccountsReceivable> list = filterReceivables(from, to, status);
        BigDecimal totalValor = sumR(list, AccountsReceivable::getTotalAmount);
        BigDecimal totalRecebido = sumR(list, AccountsReceivable::getReceivedAmount);
        BigDecimal totalSaldo = sumR(list, AccountsReceivable::getBalance);

        Context ctx = baseFinancialContext("Contas a Receber", periodSubtitle(from, to, null, status));
        ctx.setVariable("kpis", List.of(
                kpi("Registros", String.valueOf(list.size())),
                kpi("Valor Total", brl(totalValor)),
                kpi("Recebido", brl(totalRecebido)),
                kpi("Saldo em Aberto", brl(totalSaldo))
        ));
        ctx.setVariable("columns", List.of("Cliente", "Descrição", "O.S.", "Vencimento", "Pagamento", "Status", "Valor Total", "Recebido", "Saldo"));
        ctx.setVariable("numericColumnsFrom", 6);
        List<List<String>> rows = new ArrayList<>();
        for (AccountsReceivable r : list) {
            rows.add(List.of(
                    r.getClient() != null ? r.getClient().getName() : "Consumidor Final",
                    nvl(r.getDescription()), r.getWorkOrder() != null ? r.getWorkOrder().getNumber() : "—",
                    date(r.getDueDate()), date(r.getPaymentDate()), statusLabel(r.getStatus()),
                    brl(r.getTotalAmount()), brl(r.getReceivedAmount()), brl(r.getBalance())
            ));
        }
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalsRow", List.of("TOTAL", "", "", "", "", "", brl(totalValor), brl(totalRecebido), brl(totalSaldo)));

        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Contas_a_Receber.pdf");
    }

    @GetMapping("/receivables/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> receivablesExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status) {
        List<AccountsReceivable> list = filterReceivables(from, to, status);
        byte[] xls = excelReportService.receivables(list, periodSubtitle(from, to, null, status));
        return excelResponse(xls, "Relatorio_Contas_a_Receber.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // CONTAS A PAGAR
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/payables/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> payablesPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {

        List<AccountsPayable> list = filterPayables(from, to, status, category);
        BigDecimal totalValor = sumP(list, AccountsPayable::getTotalAmount);
        BigDecimal totalPago = sumP(list, AccountsPayable::getPaidAmount);
        BigDecimal totalSaldo = sumP(list, AccountsPayable::getBalance);

        Context ctx = baseFinancialContext("Contas a Pagar", periodSubtitle(from, to, null, status));
        ctx.setVariable("kpis", List.of(
                kpi("Registros", String.valueOf(list.size())),
                kpi("Valor Total", brl(totalValor)),
                kpi("Pago", brl(totalPago)),
                kpi("Saldo em Aberto", brl(totalSaldo))
        ));
        ctx.setVariable("columns", List.of("Fornecedor", "Descrição", "Categoria", "O.S.", "Vencimento", "Pagamento", "Status", "Valor Total", "Pago", "Saldo"));
        ctx.setVariable("numericColumnsFrom", 7);
        List<List<String>> rows = new ArrayList<>();
        for (AccountsPayable p : list) {
            rows.add(List.of(
                    p.getSupplier() != null ? p.getSupplier().getName() : "—",
                    nvl(p.getDescription()), nvl(p.getCategory()), p.getWorkOrder() != null ? p.getWorkOrder().getNumber() : "—",
                    date(p.getDueDate()), date(p.getPaymentDate()), statusLabel(p.getStatus()),
                    brl(p.getTotalAmount()), brl(p.getPaidAmount()), brl(p.getBalance())
            ));
        }
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalsRow", List.of("TOTAL", "", "", "", "", "", "", brl(totalValor), brl(totalPago), brl(totalSaldo)));

        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Contas_a_Pagar.pdf");
    }

    @GetMapping("/payables/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> payablesExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        List<AccountsPayable> list = filterPayables(from, to, status, category);
        byte[] xls = excelReportService.payables(list, periodSubtitle(from, to, null, status));
        return excelResponse(xls, "Relatorio_Contas_a_Pagar.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // FLUXO DE CAIXA / LIVRO CAIXA
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/cashflow/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> cashflowPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        BigDecimal opening = cashLedgerService.getOpeningBalance();
        List<CashLedgerEntryDto> entries = cashLedgerService.buildLedger(from, to, opening);
        BigDecimal totalEntradas = entries.stream().map(CashLedgerEntryDto::getEntrada).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaidas = entries.stream().map(CashLedgerEntryDto::getSaida).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoFinal = entries.isEmpty() ? opening : entries.get(entries.size() - 1).getSaldo();

        Context ctx = baseFinancialContext("Fluxo de Caixa", "Período: " + date(from) + " a " + date(to));
        ctx.setVariable("kpis", List.of(
                kpi("Saldo Inicial", brl(opening)),
                kpi("Entradas", brl(totalEntradas)),
                kpi("Saídas", brl(totalSaidas)),
                kpi("Saldo Final", brl(saldoFinal))
        ));
        ctx.setVariable("columns", List.of("Data", "Tipo", "Descrição", "Cliente/Fornecedor", "O.S.", "Forma Pgto.", "Entrada", "Saída", "Saldo"));
        ctx.setVariable("numericColumnsFrom", 6);
        List<List<String>> rows = new ArrayList<>();
        for (CashLedgerEntryDto e : entries) {
            rows.add(List.of(date(e.getDate()), e.getType() == CashLedgerEntryDto.EntryType.ENTRADA ? "Entrada" : "Saída",
                    nvl(e.getDescription()), nvl(e.getParty()), nvl(e.getWorkOrderNumber()), nvl(e.getPaymentMethod()),
                    e.getEntrada().compareTo(BigDecimal.ZERO) > 0 ? brl(e.getEntrada()) : "—",
                    e.getSaida().compareTo(BigDecimal.ZERO) > 0 ? brl(e.getSaida()) : "—", brl(e.getSaldo())));
        }
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalsRow", List.of("TOTAL", "", "", "", "", "", brl(totalEntradas), brl(totalSaidas), brl(saldoFinal)));

        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Fluxo_de_Caixa.pdf");
    }

    @GetMapping("/cashflow/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> cashflowExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        BigDecimal opening = cashLedgerService.getOpeningBalance();
        List<CashLedgerEntryDto> entries = cashLedgerService.buildLedger(from, to, opening);
        BigDecimal totalEntradas = entries.stream().map(CashLedgerEntryDto::getEntrada).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaidas = entries.stream().map(CashLedgerEntryDto::getSaida).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoFinal = entries.isEmpty() ? opening : entries.get(entries.size() - 1).getSaldo();

        byte[] xls = excelReportService.cashLedger(entries, opening, totalEntradas, totalSaidas, saldoFinal,
                "Período: " + date(from) + " a " + date(to));
        return excelResponse(xls, "Relatorio_Fluxo_de_Caixa.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // DRE
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/dre/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> drePdf(@RequestParam(defaultValue = "comparative") String type,
                                          @RequestParam(required = false) Integer month,
                                          @RequestParam(required = false) Integer year) {
        List<DreReportDto> columns = buildDre(type, month, year);

        Context ctx = baseFinancialContext("DRE Gerencial", "Tipo: " + dreTypeLabel(type));
        ctx.setVariable("columns", List.of("Período", "Receita Bruta", "Impostos", "Receita Líquida", "CMV",
                "Lucro Bruto", "Desp. Fixas", "Desp. Financeiras", "Lucro Líquido", "Margem %"));
        ctx.setVariable("numericColumnsFrom", 1);
        List<List<String>> rows = new ArrayList<>();
        for (DreReportDto d : columns) {
            rows.add(List.of(d.getMesAno(), brl(d.getReceitaBruta()), brl(d.getImpostos()), brl(d.getReceitaLiquida()),
                    brl(d.getCmv()), brl(d.getLucroBruto()), brl(d.getDespesasFixas()), brl(d.getDespesasFinanceiras()),
                    brl(d.getLucroLiquido()), pct(d.getMargemLiquida())));
        }
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalsRow", null);

        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "DRE.pdf");
    }

    @GetMapping("/dre/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> dreExcel(@RequestParam(defaultValue = "comparative") String type,
                                            @RequestParam(required = false) Integer month,
                                            @RequestParam(required = false) Integer year) {
        List<DreReportDto> columns = buildDre(type, month, year);
        byte[] xls = excelReportService.dre(columns);
        return excelResponse(xls, "DRE.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // RELATÓRIO PARA O CONTADOR
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/accountant/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> accountantPdf(@RequestParam(required = false) Integer month,
                                                  @RequestParam(required = false) Integer year) {
        LocalDate[] range = monthRange(month, year);
        List<DreReportDto> dreColumns = buildDre("single", range[0].getMonthValue(), range[0].getYear());
        List<AccountsPayable> payables = filterPayablesByPaymentDate(range[0], range[1].minusDays(1));
        List<AccountsReceivable> receivables = filterReceivablesByPaymentDate(range[0], range[1].minusDays(1));

        DreReportDto d = dreColumns.get(0);

        Context ctx = baseFinancialContext("Relatório para o Contador",
                "Competência: " + range[0].format(DateTimeFormatter.ofPattern("MMMM/yyyy", new Locale("pt", "BR"))));
        ctx.setVariable("kpis", List.of(
                kpi("Receita Bruta", brl(d.getReceitaBruta())),
                kpi("Impostos Estimados", brl(d.getImpostos())),
                kpi("CMV", brl(d.getCmv())),
                kpi("Lucro Líquido", brl(d.getLucroLiquido()))
        ));
        ctx.setVariable("columns", List.of("Período", "Receita Bruta", "Impostos Est.", "CMV", "Desp. Fixas", "Desp. Financeiras", "Lucro Líquido", "Margem %"));
        ctx.setVariable("numericColumnsFrom", 1);
        ctx.setVariable("rows", List.of(List.of(d.getMesAno(), brl(d.getReceitaBruta()), brl(d.getImpostos()), brl(d.getCmv()),
                brl(d.getDespesasFixas()), brl(d.getDespesasFinanceiras()), brl(d.getLucroLiquido()), pct(d.getMargemLiquida()))));
        ctx.setVariable("totalsRow", null);

        List<ReportSectionDto> sections = new ArrayList<>();

        List<List<String>> recRows = new ArrayList<>();
        BigDecimal totalRec = BigDecimal.ZERO;
        for (AccountsReceivable r : receivables) {
            recRows.add(List.of(r.getClient() != null ? r.getClient().getName() : "Consumidor Final",
                    nvl(r.getDescription()), date(r.getPaymentDate()), nvl(r.getPaymentMethod()), brl(r.getReceivedAmount())));
            totalRec = totalRec.add(r.getReceivedAmount());
        }
        sections.add(new ReportSectionDto("Recebimentos no Período",
                List.of("Cliente", "Descrição", "Data Pagamento", "Forma Pgto.", "Valor Recebido"),
                recRows, List.of("TOTAL RECEBIDO", "", "", "", brl(totalRec))));

        List<List<String>> payRows = new ArrayList<>();
        BigDecimal totalPay = BigDecimal.ZERO;
        for (AccountsPayable p : payables) {
            String cat = nvl(p.getCategory()) + (p.getSubcategory() != null && !p.getSubcategory().isBlank() ? " / " + p.getSubcategory() : "");
            payRows.add(List.of(p.getSupplier() != null ? p.getSupplier().getName() : "—",
                    nvl(p.getDescription()), cat, date(p.getPaymentDate()), nvl(p.getPaymentMethod()), brl(p.getPaidAmount())));
            totalPay = totalPay.add(p.getPaidAmount());
        }
        sections.add(new ReportSectionDto("Pagamentos no Período por Categoria",
                List.of("Fornecedor", "Descrição", "Categoria", "Data Pagamento", "Forma Pgto.", "Valor Pago"),
                payRows, List.of("TOTAL PAGO", "", "", "", "", brl(totalPay))));

        ctx.setVariable("extraSections", sections);
        ctx.setVariable("footNote", "Relatório gerencial gerado automaticamente pelo ERP para fins de apoio contábil. Os impostos são estimativas com base na alíquota configurada e não substituem a apuração oficial.");

        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Contador.pdf");
    }

    @GetMapping("/accountant/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> accountantExcel(@RequestParam(required = false) Integer month,
                                                    @RequestParam(required = false) Integer year) {
        LocalDate[] range = monthRange(month, year);
        List<DreReportDto> dreColumns = buildDre("single", range[0].getMonthValue(), range[0].getYear());
        List<AccountsPayable> payables = filterPayablesByPaymentDate(range[0], range[1].minusDays(1));
        List<AccountsReceivable> receivables = filterReceivablesByPaymentDate(range[0], range[1].minusDays(1));

        byte[] xls = excelReportService.accountant(dreColumns, payables, receivables,
                "Competência: " + range[0].format(DateTimeFormatter.ofPattern("MMMM/yyyy", new Locale("pt", "BR"))));
        return excelResponse(xls, "Relatorio_Contador.xlsx");
    }

    // ══════════════════════════════════════════════════════════════════
    // CADASTROS (Clientes / Fornecedores)
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/clients/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> clientsExcel() {
        List<Client> list = clientRepository.findAll();
        byte[] xls = excelReportService.clients(list);
        return excelResponse(xls, "Relatorio_Clientes.xlsx");
    }

    @GetMapping("/clients/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> clientsPdf() {
        List<Client> list = clientRepository.findAll();
        Context ctx = baseFinancialContext("Clientes", "Total: " + list.size() + " cliente(s)");
        ctx.setVariable("columns", List.of("Nome", "Tipo", "Documento", "Telefone", "E-mail", "Cidade", "Ativo"));
        ctx.setVariable("numericColumnsFrom", 99);
        List<List<String>> rows = new ArrayList<>();
        for (Client c : list) {
            rows.add(List.of(nvl(c.getName()), "company".equals(c.getType()) ? "Pessoa Jurídica" : "Pessoa Física",
                    nvl(c.getDocument()), nvl(c.getPhone()), nvl(c.getEmail()), nvl(c.getCity()),
                    Boolean.TRUE.equals(c.getIsActive()) ? "Sim" : "Não"));
        }
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalsRow", null);
        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Clientes.pdf");
    }

    @GetMapping("/suppliers/excel")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> suppliersExcel() {
        List<Supplier> list = supplierRepository.findAll();
        byte[] xls = excelReportService.suppliers(list);
        return excelResponse(xls, "Relatorio_Fornecedores.xlsx");
    }

    @GetMapping("/suppliers/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> suppliersPdf() {
        List<Supplier> list = supplierRepository.findAll();
        Context ctx = baseFinancialContext("Fornecedores", "Total: " + list.size() + " fornecedor(es)");
        ctx.setVariable("columns", List.of("Nome", "Categoria", "Documento", "Telefone", "E-mail", "Cidade", "Ativo"));
        ctx.setVariable("numericColumnsFrom", 99);
        List<List<String>> rows = new ArrayList<>();
        for (Supplier s : list) {
            rows.add(List.of(nvl(s.getName()), nvl(s.getCategory()), nvl(s.getDocument()), nvl(s.getPhone()),
                    nvl(s.getEmail()), nvl(s.getCity()), Boolean.TRUE.equals(s.getIsActive()) ? "Sim" : "Não"));
        }
        ctx.setVariable("rows", rows);
        ctx.setVariable("totalsRow", null);
        byte[] pdf = pdfReportService.render("financial-report-pdf", ctx);
        return pdfResponse(pdf, "Relatorio_Fornecedores.pdf");
    }

    // ══════════════════════════════════════════════════════════════════
    // Helpers privados
    // ══════════════════════════════════════════════════════════════════

    private Map<String, Object> toPdfRow(WorkOrderReportDto os) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("number", os.getNumber());
        row.put("clientName", os.getClientName());
        row.put("statusLabel", os.getStatusLabel());
        row.put("dateFmt", dateTime(os.getCreatedAt()));
        row.put("revenueFmt", brl(os.getTotalRevenue()));
        row.put("costFmt", brl(os.getTotalCost()));
        row.put("profitFmt", brl(os.getProfit()));
        row.put("profitPositive", os.getProfit().compareTo(BigDecimal.ZERO) >= 0);
        row.put("marginFmt", pct(os.getMarginPercent()));
        row.put("itemCount", os.getItems().size());
        return row;
    }

    private Context baseFinancialContext(String title, String subtitle) {
        Profile profile = firstProfile();
        Context ctx = new Context(new Locale("pt", "BR"));
        ctx.setVariable("title", title);
        ctx.setVariable("subtitle", subtitle);
        ctx.setVariable("companyName", nvl(profile.getCompanyName()));
        ctx.setVariable("companyDoc", nvl(profile.getDocument()));
        ctx.setVariable("companyAddress", nvl(profile.getAddress()));
        ctx.setVariable("companyEmail", nvl(profile.getEmail()));
        ctx.setVariable("companyPhone", nvl(profile.getPhone()));
        ctx.setVariable("generatedAt", dateTime(LocalDateTime.now()));
        ctx.setVariable("kpis", List.of());
        ctx.setVariable("extraSections", null);
        ctx.setVariable("footNote", null);
        return ctx;
    }

    private Map<String, String> kpi(String label, String value) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        return m;
    }

    private List<AccountsReceivable> filterReceivables(LocalDate from, LocalDate to, String status) {
        return receivableRepo.findAll().stream()
                .filter(r -> from == null || r.getDueDate() == null || !r.getDueDate().isBefore(from))
                .filter(r -> to == null || r.getDueDate() == null || !r.getDueDate().isAfter(to))
                .filter(r -> status == null || status.isBlank() || status.equalsIgnoreCase(r.getStatus()))
                .sorted(Comparator.comparing(AccountsReceivable::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /** Recebimentos efetivamente baixados (status recebido/parcial) com data de PAGAMENTO dentro do período — regime de caixa. */
    private List<AccountsReceivable> filterReceivablesByPaymentDate(LocalDate from, LocalDate to) {
        return receivableRepo.findAll().stream()
                .filter(r -> "received".equalsIgnoreCase(r.getStatus()) || "partial".equalsIgnoreCase(r.getStatus()))
                .filter(r -> r.getPaymentDate() != null && !r.getPaymentDate().isBefore(from) && !r.getPaymentDate().isAfter(to))
                .sorted(Comparator.comparing(AccountsReceivable::getPaymentDate))
                .collect(Collectors.toList());
    }

    private List<AccountsPayable> filterPayables(LocalDate from, LocalDate to, String status, String category) {
        return payableRepo.findAll().stream()
                .filter(p -> from == null || p.getDueDate() == null || !p.getDueDate().isBefore(from))
                .filter(p -> to == null || p.getDueDate() == null || !p.getDueDate().isAfter(to))
                .filter(p -> status == null || status.isBlank() || status.equalsIgnoreCase(p.getStatus()))
                .filter(p -> category == null || category.isBlank() || category.equalsIgnoreCase(p.getCategory()))
                .sorted(Comparator.comparing(AccountsPayable::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /** Pagamentos efetivamente baixados (status pago/parcial) com data de PAGAMENTO dentro do período — regime de caixa. */
    private List<AccountsPayable> filterPayablesByPaymentDate(LocalDate from, LocalDate to) {
        return payableRepo.findAll().stream()
                .filter(p -> "paid".equalsIgnoreCase(p.getStatus()) || "partial".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(from) && !p.getPaymentDate().isAfter(to))
                .sorted(Comparator.comparing(AccountsPayable::getPaymentDate))
                .collect(Collectors.toList());
    }

    private List<DreReportDto> buildDre(String type, Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM / yyyy", new Locale("pt", "BR"));

        List<DreReportDto> colunas = new ArrayList<>();
        if ("annual".equals(type)) {
            DreReportDto coluna = new DreReportDto("ANUAL " + y);
            for (int mm = 1; mm <= 12; mm++) {
                LocalDate inicio = LocalDate.of(y, mm, 1);
                LocalDate fim = inicio.plusMonths(1);
                coluna.addReceita(nvl(receivableRepo.sumReceivedByMonthAndYear(inicio, fim)));
                coluna.addCmv(nvl(payableRepo.sumCmvByMonthAndYear(inicio, fim)));
                coluna.addDespesa(nvl(payableRepo.sumDespesasFixasByMonthAndYear(inicio, fim)));
                coluna.addDespesaFinanceira(nvl(payableRepo.sumDespesasFinanceirasByMonthAndYear(inicio, fim)));
            }
            colunas.add(coluna);
        } else {
            int qtd = "single".equals(type) ? 1 : 4;
            LocalDate base = LocalDate.of(y, m, 1);
            for (int i = qtd - 1; i >= 0; i--) {
                LocalDate target = qtd == 1 ? base : base.minusMonths(i);
                LocalDate inicio = target.withDayOfMonth(1);
                LocalDate fim = inicio.plusMonths(1);
                DreReportDto coluna = new DreReportDto(target.format(fmt));
                coluna.addReceita(nvl(receivableRepo.sumReceivedByMonthAndYear(inicio, fim)));
                coluna.addCmv(nvl(payableRepo.sumCmvByMonthAndYear(inicio, fim)));
                coluna.addDespesa(nvl(payableRepo.sumDespesasFixasByMonthAndYear(inicio, fim)));
                coluna.addDespesaFinanceira(nvl(payableRepo.sumDespesasFinanceirasByMonthAndYear(inicio, fim)));
                colunas.add(coluna);
            }
        }
        return colunas;
    }

    private LocalDate[] monthRange(Integer month, Integer year) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        LocalDate inicio = LocalDate.of(y, m, 1);
        return new LocalDate[]{inicio, inicio.plusMonths(1)};
    }

    private String dreTypeLabel(String type) {
        return switch (type) {
            case "single" -> "Mensal";
            case "annual" -> "Anual";
            default -> "Comparativo (últimos 4 meses)";
        };
    }

    private String periodSubtitle(LocalDate from, LocalDate to, UUID clientId, String status) {
        StringBuilder sb = new StringBuilder();
        if (from != null || to != null) {
            sb.append("Período: ").append(from != null ? date(from) : "início").append(" a ").append(to != null ? date(to) : "hoje");
        } else {
            sb.append("Todos os períodos");
        }
        if (clientId != null) {
            clientRepository.findById(clientId).ifPresent(c -> sb.append(" · Cliente: ").append(c.getName()));
        }
        if (status != null && !status.isBlank()) {
            sb.append(" · Status: ").append(statusLabel(status));
        }
        return sb.toString();
    }

    private String statusLabel(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "pending" -> "Pendente";
            case "partial" -> "Parcial";
            case "received" -> "Recebido";
            case "paid" -> "Pago";
            case "cancelled", "canceled" -> "Cancelado";
            case "in_progress", "in-progress" -> "Em Produção";
            case "completed", "done" -> "Concluída";
            default -> status;
        };
    }

    private Profile firstProfile() {
        return profileRepository.findAll().stream().findFirst().orElseGet(Profile::new);
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private BigDecimal sumR(List<AccountsReceivable> list, java.util.function.Function<AccountsReceivable, BigDecimal> f) {
        return list.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumP(List<AccountsPayable> list, java.util.function.Function<AccountsPayable, BigDecimal> f) {
        return list.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private String nvl(String v) { return v != null && !v.isBlank() ? v : "—"; }
}
