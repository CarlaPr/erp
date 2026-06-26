package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/payables") // Rota base
public class PayableController {

    private final AccountsPayableRepository payableRepository;
    private final SupplierService supplierService;
    private final FinanceService financeService;
    private final SupplierRepository supplierRepository;
    private final WorkOrderRepository workOrderRepository;

    public PayableController(AccountsPayableRepository payableRepository, SupplierService supplierService,
                             FinanceService financeService, SupplierRepository supplierRepository,
                             WorkOrderRepository workOrderRepository) {
        this.payableRepository = payableRepository;
        this.supplierService = supplierService;
        this.financeService = financeService;
        this.supplierRepository = supplierRepository;
        this.workOrderRepository = workOrderRepository;
    }

    // Método único para listar tudo (acessado via /payables)
    @GetMapping
    public String index(Model model) {
        List<AccountsPayable> list = financeService.listAllPayables();

        // KPIs
        BigDecimal cadastrado = list.stream()
                .filter(p -> !"cancelled".equals(p.getStatus()))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pago = list.stream().filter(p -> "paid".equals(p.getStatus())).map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendente = list.stream().filter(p -> "pending".equals(p.getStatus())).map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal emAtraso = list.stream()
                .filter(p -> "pending".equals(p.getStatus()) && p.getDueDate().isBefore(LocalDate.now()))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("payables", list);
        model.addAttribute("newPayable", new AccountsPayable());
        model.addAttribute("suppliers", supplierRepository.findByIsActiveTrueOrderByNameAsc());
        model.addAttribute("workOrders", workOrderRepository.findAll());
        model.addAttribute("valCadastrado", cadastrado);
        model.addAttribute("valPago", pago);
        model.addAttribute("valPendente", pendente);
        model.addAttribute("valAtraso", emAtraso);

        return "payables";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AccountsPayable payable) {
        financeService.savePayable(payable);
        return "redirect:/payables";
    }

    @PostMapping("/pay/{id}")
    public String processPayment(@PathVariable UUID id, @RequestParam(required = false) BigDecimal amount) {
        financeService.processPayablePayment(id, amount != null ? amount : BigDecimal.ZERO);
        return "redirect:/payables";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        payableRepository.deleteById(id);
        return "redirect:/payables";
    }
}