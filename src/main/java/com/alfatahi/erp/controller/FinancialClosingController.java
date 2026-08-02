package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.FinancialClosing;
import com.alfatahi.erp.service.FinancialClosingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/financial-closing")
public class FinancialClosingController {

    private final FinancialClosingService closingService;

    public FinancialClosingController(FinancialClosingService closingService) {
        this.closingService = closingService;
    }

    @GetMapping
    public String index(Model model) {
        List<FinancialClosing> closings = closingService.listAll();
        model.addAttribute("currentPage", "financial-closing");
        model.addAttribute("closings",    closings);

        java.time.LocalDate proximoPeriodo    = closingService.proximoPeriodoParaFechar();
        boolean liberado                      = closingService.periodoLiberadoParaFechamento(proximoPeriodo);
        java.time.LocalDate dataLiberacao     = closingService.dataLiberacaoFechamento(proximoPeriodo);
        java.time.LocalDate fimProximoPeriodo = closingService.periodoFim(proximoPeriodo);

        model.addAttribute("suggestedYear",   proximoPeriodo.getYear());
        model.addAttribute("suggestedMonth",  proximoPeriodo.getMonthValue());
        model.addAttribute("canClose",        liberado);
        model.addAttribute("periodoInicio",   proximoPeriodo);
        model.addAttribute("periodoFim",      fimProximoPeriodo);
        model.addAttribute("dataLiberacao",   dataLiberacao);
        return "financial-closing";
    }

    @PostMapping("/execute")
    public String execute(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String notes,
            Principal principal,
            RedirectAttributes redirectAttrs) {

        String user = principal != null ? principal.getName() : "sistema";
        try {
            closingService.executeClosing(year, month, user, notes);
            redirectAttrs.addFlashAttribute("success",
                    "Fechamento " + String.format("%02d/%d", month, year) + " realizado com sucesso.");
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/financial-closing";
    }

    @PostMapping("/reopen-last")
    public String reopenLast(RedirectAttributes redirectAttrs) {
        try {
            closingService.reabrirUltimoFechamento();
            redirectAttrs.addFlashAttribute("success", "Último fechamento reaberto. Você já pode refazê-lo.");
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/financial-closing";
    }
}
