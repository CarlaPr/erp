package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.SupplierService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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

    public PayableController(AccountsPayableRepository payableRepository,
                             SupplierService supplierService,
                             FinanceService financeService,
                             SupplierRepository supplierRepository,
                             WorkOrderRepository workOrderRepository) {
        this.payableRepository = payableRepository;
        this.supplierService = supplierService;
        this.financeService = financeService;
        this.supplierRepository = supplierRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) UUID workOrderId,
            @RequestParam(required = false, defaultValue = "false") boolean allMonths,
            Model model) {

        if (dateFrom == null && dateTo == null && !allMonths) {
            dateFrom = LocalDate.now().withDayOfMonth(1);
            dateTo = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
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
        if (category != null && !category.isBlank()) list = list.stream().filter(p -> category.equals(p.getCategory())).collect(Collectors.toList());
        if (supplierId != null) list = list.stream().filter(p -> p.getSupplier() != null && supplierId.equals(p.getSupplier().getId())).collect(Collectors.toList());
        if (dateFrom != null) { final LocalDate df = dateFrom; list = list.stream().filter(p -> !p.getDueDate().isBefore(df)).collect(Collectors.toList()); }
        if (dateTo != null) { final LocalDate dt = dateTo; list = list.stream().filter(p -> !p.getDueDate().isAfter(dt)).collect(Collectors.toList()); }
        if (workOrderId != null) list = list.stream().filter(p -> p.getWorkOrder() != null && workOrderId.equals(p.getWorkOrder().getId())).collect(Collectors.toList());

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

        model.addAttribute("saldoReal", saldoReal);
        model.addAttribute("valTotal", total);
        model.addAttribute("valPago", pago);
        model.addAttribute("valPendente", pendente);
        model.addAttribute("valAtraso", emAtraso);

        model.addAttribute("filterSearch", search);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterSupplierId", supplierId);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("filterWorkOrderId", workOrderId);

        return "payables";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable UUID id, @ModelAttribute AccountsPayable form) {
        AccountsPayable ap = payableRepository.findById(id).orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        ap.setDescription(form.getDescription());
        ap.setCategory(form.getCategory());
        ap.setSubcategory(form.getSubcategory());
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
    public String save(@ModelAttribute AccountsPayable payable) {
        financeService.savePayable(payable);
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
        if (category != null && !category.isBlank()) list = list.stream().filter(p -> category.equals(p.getCategory())).collect(Collectors.toList());
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

        writer.println("Vencimento;Data Pagamento;Fornecedor;Categoria;O.S.;Descricao;Forma Pgto;Total;Pago;Pendente;Status");

        java.util.Locale ptBR = new java.util.Locale("pt", "BR");

        for (AccountsPayable p : list) {
            String supplierName = p.getSupplier() != null ? p.getSupplier().getName() : "Avulso";
            String osNumber = p.getWorkOrder() != null ? p.getWorkOrder().getNumber() : "-";
            String desc = p.getDescription() != null ? p.getDescription().replace(";", ",") : "";

            String formPgto = p.getPaymentMethod() != null ? p.getPaymentMethod() : "";
            String payDate = p.getPaymentDate() != null ? p.getPaymentDate().toString() : ""; // Mesma lógica da data

            writer.printf(ptBR, "%s;%s;%s;%s;%s;%s;%s;%.2f;%.2f;%.2f;%s\n",
                    p.getDueDate(), payDate, supplierName, p.getCategory(), osNumber, desc, formPgto,
                    p.getTotalAmount(), p.getPaidAmount(), p.getBalance(), p.getStatus());
        }
        writer.flush();
    }
}
