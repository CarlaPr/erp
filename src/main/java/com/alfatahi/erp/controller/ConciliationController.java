package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.BankTransaction;
import com.alfatahi.erp.repository.BankTransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/conciliation")
public class ConciliationController {

    private final BankTransactionRepository bankRepo;

    public ConciliationController(BankTransactionRepository bankRepo) {
        this.bankRepo = bankRepo;
    }

    @GetMapping
    public String index(Model model) {
        List<BankTransaction> transactions = bankRepo.findAllByOrderByTransactionDateDesc();

        BigDecimal saldoConciliado = BigDecimal.ZERO;
        BigDecimal entradasPendentes = BigDecimal.ZERO;
        BigDecimal saidasPendentes = BigDecimal.ZERO;

        for (BankTransaction t : transactions) {
            if ("conciliated".equals(t.getStatus())) {
                if ("IN".equals(t.getType())) saldoConciliado = saldoConciliado.add(t.getAmount());
                else saldoConciliado = saldoConciliado.subtract(t.getAmount());
            } else {
                if ("IN".equals(t.getType())) entradasPendentes = entradasPendentes.add(t.getAmount());
                else saidasPendentes = saidasPendentes.add(t.getAmount());
            }
        }

        model.addAttribute("currentPage", "conciliation");
        model.addAttribute("transactions", transactions);
        model.addAttribute("saldoConciliado", saldoConciliado);
        model.addAttribute("entradasPendentes", entradasPendentes);
        model.addAttribute("saidasPendentes", saidasPendentes);
        model.addAttribute("newTx", new BankTransaction());

        return "conciliation";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newTx") BankTransaction tx) {
        bankRepo.save(tx);
        return "redirect:/conciliation";
    }

    @GetMapping("/toggle/{id}")
    public String toggleStatus(@PathVariable("id") UUID id) {
        BankTransaction tx = bankRepo.findById(id).orElse(null);
        if (tx != null) {
            tx.setStatus("conciliated".equals(tx.getStatus()) ? "pending" : "conciliated");
            bankRepo.save(tx);
        }
        return "redirect:/conciliation";
    }
}