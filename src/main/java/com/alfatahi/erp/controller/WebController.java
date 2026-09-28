package com.alfatahi.erp.controller;

import com.alfatahi.erp.repository.DashboardRepository;
import com.alfatahi.erp.repository.LossRepository;
import com.alfatahi.erp.service.FinancialPeriod;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class WebController {
    private final DashboardRepository dashboardRepository;
    private final LossRepository lossRepository;

    public WebController(DashboardRepository dashboardRepository, LossRepository lossRepository) {
        this.dashboardRepository = dashboardRepository;
        this.lossRepository = lossRepository;
    }

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String month,
                            @RequestParam(name = "mes", required = false) Integer mes,
                            @RequestParam(name = "ano", required = false) Integer ano, Model model) {
        LocalDate today = FinancialPeriod.today();
        YearMonth currentReference = FinancialPeriod.referenceFor(today);
        if (mes == null || mes < 1 || mes > 12) mes = currentReference.getMonthValue();
        if (ano == null || ano < 2000 || ano > 2100) ano = currentReference.getYear();
        FinancialPeriod period = month == null || month.isBlank()
                ? FinancialPeriod.monthly(YearMonth.of(ano, mes))
                : FinancialPeriod.select(month, false, null, null);
        if (period.reference() == null) period = FinancialPeriod.current();
        mes = period.reference().getMonthValue();
        ano = period.reference().getYear();
        period.addTo(model);

        var totals = dashboardRepository.periodTotals(period, today);
        model.addAllAttributes(totals);
        BigDecimal pendingIncome = (BigDecimal) totals.get("totalAReceber");
        BigDecimal pendingExpenses = (BigDecimal) totals.get("totalAPagar");
        BigDecimal balance = (BigDecimal) totals.get("saldoAtual");
        model.addAttribute("receitasFuturas", pendingIncome);
        model.addAttribute("saldoProjetado", balance.add(pendingIncome).subtract(pendingExpenses));

        var months = dashboardRepository.annualTotals(ano);
        BigDecimal revenue = months.get(mes - 1).revenue();
        model.addAttribute("resumoAnual", months);
        model.addAttribute("faturamentoMes", revenue);
        model.addAttribute("osPorFormaPagamento", dashboardRepository.paymentMethods(period));

        BigDecimal losses = lossRepository.sumFinancialImpactByPeriod(period.from(), period.endExclusive());
        if (losses == null) losses = BigDecimal.ZERO;
        model.addAttribute("margemPerdas", revenue.signum() > 0
                ? losses.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        long quotes = ((Number) totals.get("totalOrcamentos")).longValue();
        long approved = ((Number) totals.get("orcamentosAprovados")).longValue();
        model.addAttribute("taxaConversao", quotes > 0
                ? BigDecimal.valueOf(approved).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(quotes), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);
        return "dashboard";
    }
}
