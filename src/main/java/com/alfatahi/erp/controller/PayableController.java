package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.RecurrenceEndType;
import com.alfatahi.erp.entity.RecurrenceFrequency;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.CategoryCatalogService;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.RecurrenceService;
import com.alfatahi.erp.service.SupplierService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/payables")
public class PayableController {

    private final AccountsPayableRepository payableRepository;
    private final SupplierService supplierService;
    private final FinanceService financeService;
    private final SupplierRepository supplierRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final RecurrenceService recurrenceService;

    public PayableController(AccountsPayableRepository payableRepository,
                             SupplierService supplierService,
                             FinanceService financeService,
                             SupplierRepository supplierRepository,
                             WorkOrderRepository workOrderRepository,
                             CategoryCatalogService categoryCatalogService,
                             RecurrenceService recurrenceService) {
        this.payableRepository = payableRepository;
        this.supplierService = supplierService;
        this.financeService = financeService;
        this.supplierRepository = supplierRepository;
        this.workOrderRepository = workOrderRepository;
        this.categoryCatalogService = categoryCatalogService;
        this.recurrenceService = recurrenceService;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) String costCenter,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false, defaultValue = "todas") String aba,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID workOrderId,
            @RequestParam(required = false, defaultValue = "false") boolean allMonths,
            Model model) {

        if (dateFrom == null && dateTo == null && !allMonths) {
            LocalDate today = LocalDate.now();
            dateFrom = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            dateTo = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        List<AccountsPayable> list = financeService.listAllPayables().stream()
                .filter(p -> !("cancelled".equals(p.getStatus()) || "inactive".equals(p.getStatus()))
                        || ("cancelled".equals(status) || "inactive".equals(status)))
                .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream().filter(p -> p.getDescription().toLowerCase().contains(q) || (p.getSupplier() != null && p.getSupplier().getName().toLowerCase().contains(q)) || (p.getDocumentNumber() != null && p.getDocumentNumber().toLowerCase().contains(q))).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) list = list.stream().filter(p -> status.equals(p.getStatus())).collect(Collectors.toList());
        if (category != null && !category.isBlank()) list = list.stream().filter(p -> category.equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
        if (subcategory != null && !subcategory.isBlank()) list = list.stream().filter(p -> subcategory.equalsIgnoreCase(p.getSubcategory())).collect(Collectors.toList());
        if (costCenter != null && !costCenter.isBlank()) {
            String q = costCenter.toLowerCase();
            list = list.stream().filter(p -> p.getCostCenter() != null && p.getCostCenter().toLowerCase().contains(q)).collect(Collectors.toList());
        }
        if (competencia != null && !competencia.isBlank()) {
            YearMonth parsed;
            try {
                parsed = YearMonth.parse(competencia);
            } catch (Exception e) {
                parsed = null;
            }
            if (parsed != null) {
                final YearMonth ymFinal = parsed;
                list = list.stream().filter(p -> p.getCompetencia() != null && YearMonth.from(p.getCompetencia()).equals(ymFinal)).collect(Collectors.toList());
            }
        }
        if (supplierId != null) list = list.stream().filter(p -> p.getSupplier() != null && supplierId.equals(p.getSupplier().getId())).collect(Collectors.toList());
        if (dateFrom != null) { final LocalDate df = dateFrom; list = list.stream().filter(p -> !p.getDueDate().isBefore(df)).collect(Collectors.toList()); }
        if (dateTo != null) { final LocalDate dt = dateTo; list = list.stream().filter(p -> !p.getDueDate().isAfter(dt)).collect(Collectors.toList()); }
        if (workOrderId != null) list = list.stream().filter(p -> p.getWorkOrder() != null && workOrderId.equals(p.getWorkOrder().getId())).collect(Collectors.toList());

        switch (aba == null ? "todas" : aba) {
            case "fixas" -> list = list.stream().filter(p -> "FIXA".equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
            case "recorrentes" -> list = list.stream().filter(p -> Boolean.TRUE.equals(p.getRecurring())).collect(Collectors.toList());
            case "vencidas" -> list = list.stream().filter(AccountsPayable::isOverdue).collect(Collectors.toList());
            case "pagas" -> list = list.stream().filter(p -> "paid".equals(p.getStatus())).collect(Collectors.toList());
            case "provisionamentos" -> list = list.stream().filter(p -> "PROVISIONAMENTO".equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
            default -> {  }
        }

        BigDecimal totalEntradasGeral = financeService.listAllReceivables().stream().filter(r -> "received".equals(r.getStatus()) || "partial".equals(r.getStatus())).map(AccountsReceivable::getNetReceivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaidasGeral = financeService.listAllPayables().stream().filter(p -> "paid".equals(p.getStatus()) || "partial".equals(p.getStatus())).map(AccountsPayable::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoReal = totalEntradasGeral.subtract(totalSaidasGeral);

        BigDecimal total = list.stream().map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pago = list.stream().filter(p -> "paid".equals(p.getStatus()) || "partial".equals(p.getStatus())).map(AccountsPayable::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendente = list.stream().filter(p -> "pending".equals(p.getStatus()) || "partial".equals(p.getStatus())).map(AccountsPayable::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal emAtraso = list.stream().filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus())) && p.getDueDate().isBefore(LocalDate.now())).map(AccountsPayable::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentPage", "payables");
        model.addAttribute("payables", list);
        model.addAttribute("newPayable", new AccountsPayable());
        model.addAttribute("suppliers", supplierRepository.findByIsActiveTrueOrderByNameAsc());
        model.addAttribute("workOrders", workOrderRepository.findAll());
        model.addAttribute("categories", categoryCatalogService.listActiveCategories());

        model.addAttribute("saldoReal", saldoReal);
        model.addAttribute("valTotal", total);
        model.addAttribute("valPago", pago);
        model.addAttribute("valPendente", pendente);
        model.addAttribute("valAtraso", emAtraso);

        model.addAttribute("filterSearch", search);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterSubcategory", subcategory);
        model.addAttribute("filterCostCenter", costCenter);
        model.addAttribute("filterCompetencia", competencia);
        model.addAttribute("filterAba", aba);
        model.addAttribute("filterSupplierId", supplierId);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("filterWorkOrderId", workOrderId);

        return "payables";
    }


    @Transactional(readOnly = true)
    @GetMapping("/categories-data")
    @ResponseBody
    public Map<String, Object> categoriesData() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("categories", categoryCatalogService.listActiveCategories());
        result.put("subcategoryTree", categoryCatalogService.buildCategoryTree());
        return result;
    }


    @Transactional(readOnly = true)
    @GetMapping("/fixed")
    public String fixedAccounts(Model model) {
        LocalDate hoje = LocalDate.now();
        YearMonth ym = YearMonth.from(hoje);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate fimMes = ym.atEndOfMonth().plusDays(1);

        List<AccountsPayable> fixasOuRecorrentes = financeService.listAllPayables().stream()
                .filter(p -> !"cancelled".equals(p.getStatus()))
                .filter(p -> "FIXA".equalsIgnoreCase(p.getCategory()) || Boolean.TRUE.equals(p.getRecurring()))
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .collect(Collectors.toList());

        List<AccountsPayable> doMesAtual = fixasOuRecorrentes.stream()
                .filter(p -> !p.getDueDate().isBefore(inicioMes) && p.getDueDate().isBefore(fimMes))
                .collect(Collectors.toList());

        List<AccountsPayable> vencidas = fixasOuRecorrentes.stream()
                .filter(AccountsPayable::isOverdue)
                .collect(Collectors.toList());

        List<AccountsPayable> pagas = fixasOuRecorrentes.stream()
                .filter(p -> "paid".equals(p.getStatus()))
                .sorted((a, b) -> {
                    LocalDate da = a.getPaymentDate() != null ? a.getPaymentDate() : a.getDueDate();
                    LocalDate db = b.getPaymentDate() != null ? b.getPaymentDate() : b.getDueDate();
                    return db.compareTo(da);
                })
                .limit(50)
                .collect(Collectors.toList());

        List<AccountsPayable> futuras = fixasOuRecorrentes.stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus())) && !p.getDueDate().isBefore(fimMes))
                .collect(Collectors.toList());

        BigDecimal totalMes = fixasOuRecorrentes.stream()
                .filter(p -> !p.getDueDate().isBefore(inicioMes) && p.getDueDate().isBefore(fimMes))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagoMes = fixasOuRecorrentes.stream()
                .filter(p -> ("paid".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && !p.getDueDate().isBefore(inicioMes) && p.getDueDate().isBefore(fimMes))
                .map(AccountsPayable::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPendenteMes = fixasOuRecorrentes.stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && !p.getDueDate().isBefore(inicioMes) && p.getDueDate().isBefore(fimMes))
                .map(AccountsPayable::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AccountsPayable> proximosVencimentos = fixasOuRecorrentes.stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus())) && !p.getDueDate().isBefore(hoje))
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("currentPage", "payables");
        model.addAttribute("mesAtualLabel", String.format("%02d/%d", hoje.getMonthValue(), hoje.getYear()));
        model.addAttribute("doMesAtual", doMesAtual);
        model.addAttribute("vencidas", vencidas);
        model.addAttribute("pagas", pagas);
        model.addAttribute("futuras", futuras);
        model.addAttribute("totalMes", totalMes);
        model.addAttribute("totalPagoMes", totalPagoMes);
        model.addAttribute("totalPendenteMes", totalPendenteMes);
        model.addAttribute("proximosVencimentos", proximosVencimentos);

        return "fixed-accounts";
    }


    @PostMapping("/edit/{id}")
    public String edit(@PathVariable UUID id, @ModelAttribute AccountsPayable form,
                       @RequestParam(required = false, defaultValue = "single") String editScope) {
        AccountsPayable ap = payableRepository.findById(id).orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        applyDerivedFields(form);

        if ("series".equals(editScope) && ap.getRecurrenceId() != null) {
            recurrenceService.editEntireSeries(ap.getRecurrenceId(), form);
            return "redirect:/payables";
        }

        ap.setDescription(form.getDescription());
        ap.setCategory(form.getCategory());
        ap.setSubcategory(form.getSubcategory());
        ap.setExpenseType(form.getExpenseType());
        ap.setExpenseNature(form.getExpenseNature());
        ap.setCostCenter(form.getCostCenter());
        ap.setCompetencia(form.getCompetencia());
        ap.setTotalAmount(form.getTotalAmount());
        ap.setDueDate(form.getDueDate());
        ap.setPaymentMethod(form.getPaymentMethod());
        ap.setDocumentNumber(form.getDocumentNumber());
        ap.setRecurring(form.getRecurring());
        ap.setNotes(form.getNotes());

        if (form.getPaidAmount() != null) {
            ap.setPaidAmount(form.getPaidAmount());
            if (ap.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
                ap.setStatus("pending");
            } else if (ap.getPaidAmount().compareTo(ap.getTotalAmount()) >= 0) {
                ap.setStatus("paid");
            } else {
                ap.setStatus("partial");
            }
        }

        if (form.getSupplier() != null && form.getSupplier().getId() != null) ap.setSupplier(form.getSupplier());
        else ap.setSupplier(null);

        if (form.getWorkOrder() != null && form.getWorkOrder().getId() != null) ap.setWorkOrder(form.getWorkOrder());
        else ap.setWorkOrder(null);

        payableRepository.save(ap);
        return "redirect:/payables";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccountsPayable payable,
                       @RequestParam(required = false, defaultValue = "false") boolean recurrenceEnabled,
                       @RequestParam(required = false) String recurrenceFrequency,
                       @RequestParam(required = false) String recurrenceEndType,
                       @RequestParam(required = false) Integer recurrenceCount,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recurrenceEndDate) {

        applyDerivedFields(payable);

        if (recurrenceEnabled && recurrenceFrequency != null && !recurrenceFrequency.isBlank()) {
            RecurrenceFrequency freq;
            RecurrenceEndType endType;
            try {
                freq = RecurrenceFrequency.valueOf(recurrenceFrequency);
                endType = RecurrenceEndType.valueOf(
                        recurrenceEndType != null && !recurrenceEndType.isBlank() ? recurrenceEndType : "INFINITE");
            } catch (IllegalArgumentException e) {
                return "redirect:/payables?error=invalid_recurrence";
            }
            payable.setRecurring(true);
            recurrenceService.createRecurrence(payable, freq, endType, recurrenceCount, recurrenceEndDate);
        } else {
            financeService.savePayable(payable);
        }
        return "redirect:/payables";
    }

    @PostMapping("/pay/{id}")
    public String processPayment(
            @PathVariable UUID id,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String notes) {

        AccountsPayable ap = payableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        BigDecimal toPayAmount = (amount != null) ? amount : ap.getBalance();

        if (toPayAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/payables?error=invalid_amount";
        }

        financeService.processPayablePayment(id, toPayAmount, paymentDate, paymentMethod, notes);
        return "redirect:/payables";
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable UUID id) {
        AccountsPayable ap = payableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        ap.setStatus("cancelled");
        payableRepository.save(ap);
        return "redirect:/payables";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        return cancel(id);
    }


    @PostMapping("/recurrence/delete-single/{id}")
    public String deleteRecurrenceSingle(@PathVariable UUID id) {
        recurrenceService.deleteSingleInstallment(id);
        return "redirect:/payables";
    }

    @PostMapping("/recurrence/delete-future/{id}")
    public String deleteRecurrenceFuture(@PathVariable UUID id) {
        recurrenceService.deleteCurrentAndFuture(id);
        return "redirect:/payables";
    }

    @PostMapping("/recurrence/delete-series/{recurrenceId}")
    public String deleteRecurrenceSeries(@PathVariable UUID recurrenceId) {
        recurrenceService.deleteEntireSeries(recurrenceId);
        return "redirect:/payables";
    }

    @PostMapping("/recurrence/cancel-future/{recurrenceId}")
    public String cancelRecurrenceFuture(@PathVariable UUID recurrenceId) {
        recurrenceService.cancelFutureOccurrences(recurrenceId);
        return "redirect:/payables";
    }

    @Transactional(readOnly = true)
    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID workOrderId,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {

        List<AccountsPayable> list = financeService.listAllPayables().stream()
                .filter(p -> !("cancelled".equals(p.getStatus()) || "inactive".equals(p.getStatus()))
                        || ("cancelled".equals(status) || "inactive".equals(status)))
                .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream().filter(p ->
                    p.getDescription().toLowerCase().contains(q)
                            || (p.getSupplier() != null && p.getSupplier().getName().toLowerCase().contains(q))
                            || (p.getDocumentNumber() != null && p.getDocumentNumber().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) list = list.stream().filter(p -> status.equals(p.getStatus())).collect(Collectors.toList());
        if (category != null && !category.isBlank()) list = list.stream().filter(p -> category.equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
        if (supplierId != null) list = list.stream().filter(p -> p.getSupplier() != null && supplierId.equals(p.getSupplier().getId())).collect(Collectors.toList());
        if (dateFrom != null) list = list.stream().filter(p -> !p.getDueDate().isBefore(dateFrom)).collect(Collectors.toList());
        if (dateTo != null) list = list.stream().filter(p -> !p.getDueDate().isAfter(dateTo)).collect(Collectors.toList());
        if (workOrderId != null) list = list.stream().filter(p -> p.getWorkOrder() != null && workOrderId.equals(p.getWorkOrder().getId())).collect(Collectors.toList());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"contas_a_pagar.csv\"");
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        writer.println("Vencimento;Data Pagamento;Fornecedor;Categoria;Subcategoria;Centro de Custo;Competencia;O.S.;Descricao;Forma Pgto;Total;Pago;Pendente;Status");
        java.util.Locale ptBR = new java.util.Locale("pt", "BR");

        for (AccountsPayable p : list) {
            String supplierName = p.getSupplier() != null ? p.getSupplier().getName() : "Avulso";
            String osNumber = p.getWorkOrder() != null ? p.getWorkOrder().getNumber() : "-";
            String desc = p.getDescription() != null ? p.getDescription().replace(";", ",") : "";
            String formPgto = p.getPaymentMethod() != null ? p.getPaymentMethod() : "";
            String payDate = p.getPaymentDate() != null ? p.getPaymentDate().toString() : "";
            String subcat = p.getSubcategory() != null ? p.getSubcategory() : "";
            String cc = p.getCostCenter() != null ? p.getCostCenter() : "";
            String comp = p.getCompetencia() != null ? p.getCompetencia().toString() : "";

            writer.printf(ptBR, "%s;%s;%s;%s;%s;%s;%s;%s;%s;%s;%.2f;%.2f;%.2f;%s\n",
                    p.getDueDate(), payDate, supplierName, p.getCategory(), subcat, cc, comp, osNumber, desc, formPgto,
                    p.getTotalAmount(), p.getPaidAmount(), p.getBalance(), p.getStatus());
        }
        writer.flush();
    }



    private void applyDerivedFields(AccountsPayable p) {
        if (p.getExpenseType() == null || p.getExpenseType().isBlank()) {
            p.setExpenseType("VARIAVEL".equalsIgnoreCase(p.getCategory()) ? "VARIAVEL" : "FIXA");
        }
        if (p.getCompetencia() == null && p.getDueDate() != null) {
            p.setCompetencia(p.getDueDate().withDayOfMonth(1));
        }
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}