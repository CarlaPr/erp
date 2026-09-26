package com.alfatahi.erp.controller;

import com.alfatahi.erp.service.FinancialPeriod;
import com.alfatahi.erp.service.CashLedgerService;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.ExpenseAllocation;
import com.alfatahi.erp.entity.RecurrenceEndType;
import com.alfatahi.erp.entity.RecurrenceFrequency;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.ExpenseAllocationRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.CategoryCatalogService;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.RecurrenceService;
import com.alfatahi.erp.service.SupplierService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final CashLedgerService cashLedgerService;
    private final SupplierRepository supplierRepository;
    private final WorkOrderRepository workOrderRepository;
    private final CategoryCatalogService categoryCatalogService;
    private final RecurrenceService recurrenceService;
    private final ExpenseAllocationRepository expenseAllocationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PayableController(AccountsPayableRepository payableRepository,
                             SupplierService supplierService,
                             FinanceService financeService,
                             CashLedgerService cashLedgerService,
                             SupplierRepository supplierRepository,
                             WorkOrderRepository workOrderRepository,
                             CategoryCatalogService categoryCatalogService,
                             RecurrenceService recurrenceService,
                             ExpenseAllocationRepository expenseAllocationRepository) {
        this.payableRepository = payableRepository;
        this.supplierService = supplierService;
        this.financeService = financeService;
        this.cashLedgerService = cashLedgerService;
        this.supplierRepository = supplierRepository;
        this.workOrderRepository = workOrderRepository;
        this.categoryCatalogService = categoryCatalogService;
        this.recurrenceService = recurrenceService;
        this.expenseAllocationRepository = expenseAllocationRepository;
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
            @RequestParam(required = false) String month,
            Model model) {

        FinancialPeriod period = FinancialPeriod.select(
                month != null && !month.isBlank() ? month : competencia, allMonths, dateFrom, dateTo);
        period.addTo(model);
        dateFrom = period.from();
        dateTo = period.to();

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
        if (supplierId != null) list = list.stream().filter(p -> p.getSupplier() != null && supplierId.equals(p.getSupplier().getId())).collect(Collectors.toList());
        list = list.stream().filter(p -> period.contains(p.getDueDate())).collect(Collectors.toList());
        if (workOrderId != null) list = list.stream().filter(p -> p.getWorkOrder() != null && workOrderId.equals(p.getWorkOrder().getId())).collect(Collectors.toList());

        switch (aba == null ? "todas" : aba) {
            case "fixas" -> list = list.stream().filter(p -> "FIXA".equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
            case "recorrentes" -> list = list.stream().filter(p -> Boolean.TRUE.equals(p.getRecurring())).collect(Collectors.toList());
            case "vencidas" -> list = list.stream().filter(AccountsPayable::isOverdue).collect(Collectors.toList());
            case "pagas" -> list = list.stream().filter(p -> "paid".equals(p.getStatus())).collect(Collectors.toList());
            case "provisionamentos" -> list = list.stream().filter(p -> "PROVISIONAMENTO".equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
            default -> {  }
        }

        BigDecimal saldoReal = period.to() == null ? cashLedgerService.getCurrentBalances().total()
                : cashLedgerService.getOpeningBalances(period.endExclusive()).total();

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
        model.addAttribute("allocationsJsonByPayable", buildAllocationsJsonByPayable(list));

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
    public String fixedAccounts(@RequestParam(required = false) String month, Model model) {
        FinancialPeriod period = FinancialPeriod.select(month, false, null, null);
        if (period.reference() == null) period = FinancialPeriod.current();
        period.addTo(model);
        LocalDate hoje = FinancialPeriod.today();
        LocalDate inicioMes = period.from();
        LocalDate fimMes = period.endExclusive();

        List<AccountsPayable> fixasOuRecorrentes = financeService.listAllPayables().stream()
                .filter(p -> !"cancelled".equals(p.getStatus()))
                .filter(p -> "FIXA".equalsIgnoreCase(p.getCategory()) || Boolean.TRUE.equals(p.getRecurring()))
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .collect(Collectors.toList());

        List<AccountsPayable> doMesAtual = fixasOuRecorrentes.stream()
                .filter(p -> !p.getDueDate().isBefore(inicioMes) && p.getDueDate().isBefore(fimMes))
                .collect(Collectors.toList());

        List<AccountsPayable> vencidas = doMesAtual.stream()
                .filter(AccountsPayable::isOverdue)
                .collect(Collectors.toList());

        List<AccountsPayable> pagas = doMesAtual.stream()
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

        List<AccountsPayable> proximosVencimentos = doMesAtual.stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus())) && !p.getDueDate().isBefore(hoje))
                .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                .limit(5)
                .collect(Collectors.toList());

        model.addAttribute("currentPage", "payables");
        model.addAttribute("mesAtualLabel", period.label());
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
                       @RequestParam(required = false, defaultValue = "single") String editScope,
                       @RequestParam(required = false, defaultValue = "false") boolean allocationsSubmitted,
                       @RequestParam(required = false) List<UUID> workOrderIds,
                       @RequestParam(required = false) List<BigDecimal> workOrderValues,
                       @RequestParam(required = false) List<String> workOrderDescriptions) {
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
        if (allocationsSubmitted) {
            applyWorkOrderAllocations(ap.getId(), workOrderIds, workOrderValues, workOrderDescriptions);
        }
        return "redirect:/payables";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccountsPayable payable,
                       @RequestParam(required = false, defaultValue = "false") boolean recurrenceEnabled,
                       @RequestParam(required = false) String recurrenceFrequency,
                       @RequestParam(required = false) String recurrenceEndType,
                       @RequestParam(required = false) Integer recurrenceCount,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recurrenceEndDate,
                       @RequestParam(required = false, defaultValue = "false") boolean allocationsSubmitted,
                       @RequestParam(required = false) List<UUID> workOrderIds,
                       @RequestParam(required = false) List<BigDecimal> workOrderValues,
                       @RequestParam(required = false) List<String> workOrderDescriptions) {

        applyDerivedFields(payable);

        UUID payableIdForAllocation;
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
            // Contas recorrentes: o rateio por OS se aplica apenas ao primeiro lançamento gerado
            // agora (o do vencimento informado), não é replicado nas ocorrências futuras.
            List<AccountsPayable> created = recurrenceService.createRecurrence(payable, freq, endType, recurrenceCount, recurrenceEndDate);
            payableIdForAllocation = created.isEmpty() ? null : created.get(0).getId();
        } else {
            payableIdForAllocation = financeService.savePayable(payable).getId();
        }

        if (allocationsSubmitted) {
            applyWorkOrderAllocations(payableIdForAllocation, workOrderIds, workOrderValues, workOrderDescriptions);
        }
        return "redirect:/payables";
    }

    /**
     * Aplica o rateio por OS informado no formulário (múltiplas OS com valor
     * individual cada), substituindo o rateio anterior da conta. Só é chamado
     * quando o formulário efetivamente enviou o bloco de rateio
     * (allocationsSubmitted=true) — contas antigas, salvas antes deste
     * recurso existir, não são tocadas e por isso nunca recebem custo
     * automático retroativo.
     */
    private void applyWorkOrderAllocations(UUID payableId, List<UUID> workOrderIds,
                                           List<BigDecimal> workOrderValues, List<String> workOrderDescriptions) {
        if (payableId == null) return;
        if (workOrderIds == null) workOrderIds = List.of();

        List<FinanceService.AllocationInput> inputs = new ArrayList<>();
        for (int i = 0; i < workOrderIds.size(); i++) {
            UUID woId = workOrderIds.get(i);
            if (woId == null) continue;
            BigDecimal value = (workOrderValues != null && i < workOrderValues.size()) ? workOrderValues.get(i) : null;
            String description = (workOrderDescriptions != null && i < workOrderDescriptions.size()) ? workOrderDescriptions.get(i) : null;
            inputs.add(new FinanceService.AllocationInput(woId, value, description));
        }
        financeService.replaceAllocations(payableId, inputs);
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
        // cancelPayable também remove as alocações de OS e os custos que elas
        // haviam lançado, evitando que um custo fique "órfão" na OS.
        financeService.cancelPayable(id);
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
            @RequestParam(required = false) String month,
            @RequestParam(required = false, defaultValue = "false") boolean allMonths,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        FinancialPeriod period = FinancialPeriod.select(month, allMonths, dateFrom, dateTo);

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
        list = list.stream().filter(p -> period.contains(p.getDueDate())).collect(Collectors.toList());
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



    /**
     * Monta, para cada conta a pagar listada, o JSON com o rateio por O.S.
     * já cadastrado (usado para pré-preencher o modal de edição). Contas sem
     * nenhuma alocação simplesmente retornam uma lista vazia "[]".
     */
    private Map<UUID, String> buildAllocationsJsonByPayable(List<AccountsPayable> list) {
        Map<UUID, String> result = new LinkedHashMap<>();
        if (list.isEmpty()) return result;

        List<UUID> payableIds = list.stream().map(AccountsPayable::getId).toList();
        Map<UUID, List<Map<String, Object>>> rowsByPayable = new LinkedHashMap<>();

        for (ExpenseAllocation ea : expenseAllocationRepository.findByAccountsPayableIdIn(payableIds)) {
            if (ea.getWorkOrder() == null || ea.getAccountsPayable() == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("workOrderId", ea.getWorkOrder().getId().toString());
            row.put("workOrderNumber", ea.getWorkOrder().getNumber());
            row.put("workOrderTitle", ea.getWorkOrder().getTitle());
            row.put("value", ea.getValue());
            row.put("description", ea.getDescription());
            rowsByPayable.computeIfAbsent(ea.getAccountsPayable().getId(), k -> new ArrayList<>()).add(row);
        }

        for (UUID payableId : payableIds) {
            List<Map<String, Object>> rows = rowsByPayable.getOrDefault(payableId, List.of());
            try {
                result.put(payableId, objectMapper.writeValueAsString(rows));
            } catch (Exception e) {
                result.put(payableId, "[]");
            }
        }
        return result;
    }

    private void applyDerivedFields(AccountsPayable p) {
        if (p.getExpenseType() == null || p.getExpenseType().isBlank()) {
            p.setExpenseType("VARIAVEL".equalsIgnoreCase(p.getCategory()) ? "VARIAVEL" : "FIXA");
        }
        if (p.getCompetencia() == null && p.getDueDate() != null) {
            p.setCompetencia(FinancialPeriod.referenceFor(p.getDueDate()).atDay(1));
        }
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}