package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@Controller
@RequestMapping("/finance")
public class FinanceController {

    private final FinanceService financeService;
    private final ClientService clientService;
    private final SupplierService supplierService;

    public FinanceController(FinanceService financeService, ClientService clientService, SupplierService supplierService) {
        this.financeService = financeService;
        this.clientService = clientService;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String getCashFlow(Model model) {
        financeService.createDefaultAccountIfEmpty(); // Auto setup do caixa inicial

        BigDecimal totalIn = financeService.getTotalReceivables();
        BigDecimal totalOut = financeService.getTotalPayables();
        BigDecimal balance = totalIn.subtract(totalOut);

        model.addAttribute("currentPage", "finance");
        model.addAttribute("payables", financeService.listAllPayables());
        model.addAttribute("receivables", financeService.listAllReceivables());
        model.addAttribute("accounts", financeService.listAllAccounts());
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("suppliers", supplierService.listAllActive());

        model.addAttribute("totalIn", totalIn);
        model.addAttribute("totalOut", totalOut);
        model.addAttribute("balance", balance);

        model.addAttribute("newPayable", new AccountsPayable());
        model.addAttribute("newReceivable", new AccountsReceivable());

        return "cash-flow";
    }

    @PostMapping("/payable/save")
    public String savePayable(@ModelAttribute("newPayable") AccountsPayable p) {
        financeService.savePayable(p);
        return "redirect:/finance";
    }

    @PostMapping("/receivable/save")
    public String saveReceivable(@ModelAttribute("newReceivable") AccountsReceivable r) {
        financeService.saveReceivable(r);
        return "redirect:/finance";
    }
}