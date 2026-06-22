package com.alfatahi.erp.controller;

import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.math.BigDecimal;

@Controller
public class WebController {

    private final FinanceService financeService;
    private final WorkOrderService workOrderService;

    public WebController(FinanceService financeService, WorkOrderService workOrderService) {
        this.financeService = financeService;
        this.workOrderService = workOrderService;
    }

    @GetMapping("/")
    public String root() { return "redirect:/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");

        // KPIs Financeiros Básicos
        BigDecimal receitas = financeService.getTotalReceivables();
        BigDecimal despesas = financeService.getTotalPayables();
        model.addAttribute("totalReceitas", receitas);
        model.addAttribute("totalDespesas", despesas);
        model.addAttribute("saldoAtual", receitas.subtract(despesas));

        // Em um cenário real, estas seriam queries agrupadas por mês do Repository.
        // Simulando arrays de dados para o Chart.js:
        model.addAttribute("chartMonths", "['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun']");
        model.addAttribute("chartRevenues", "[" + receitas + ", 4500, 6000, 8000, 7500, 9200]");
        model.addAttribute("chartExpenses", "[" + despesas + ", 3000, 4100, 4500, 5000, 4800]");

        return "dashboard";
    }
}