package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/payables")
public class PayableController {

    private final AccountsPayableRepository payableRepository;
    private final SupplierService supplierService;

    public PayableController(AccountsPayableRepository payableRepository, SupplierService supplierService) {
        this.payableRepository = payableRepository;
        this.supplierService = supplierService;
    }

    @GetMapping
    public String index(Model model) {
        List<AccountsPayable> list = payableRepository.findAllByOrderByDueDateAsc();

        // KPIs
        BigDecimal cadastrado = list.stream().map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pago = list.stream().filter(p -> "paid".equals(p.getStatus())).map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendente = list.stream().filter(p -> "pending".equals(p.getStatus())).map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal emAtraso = list.stream()
                .filter(p -> "pending".equals(p.getStatus()) && p.getDueDate().isBefore(LocalDate.now()))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentPage", "payables");
        model.addAttribute("payables", list);
        model.addAttribute("suppliers", supplierService.listAllActive());
        model.addAttribute("newPayable", new AccountsPayable());

        model.addAttribute("valCadastrado", cadastrado);
        model.addAttribute("valPago", pago);
        model.addAttribute("valPendente", pendente);
        model.addAttribute("valAtraso", emAtraso);

        return "payables";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newPayable") AccountsPayable payable) {
        payableRepository.save(payable);
        return "redirect:/payables";
    }
}