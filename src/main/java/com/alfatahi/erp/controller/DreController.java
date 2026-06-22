package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.DreReportDto;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.service.FinanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequestMapping("/dre")
public class DreController {

    private final FinanceService financeService;

    public DreController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping
    public String generateDre(Model model) {
        DreReportDto dre = new DreReportDto();

        // 1. Soma de Receitas Recebidas
        BigDecimal receitas = financeService.listAllReceivables().stream()
                .filter(r -> "received".equals(r.getStatus()))
                .map(AccountsReceivable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dre.setReceitasBrutas(receitas);

        // 2. Separação de Custos por Classificação
        BigDecimal variaveis = financeService.listAllPayables().stream()
                .filter(p -> "paid".equals(p.getStatus()) && "variable".equals(p.getCategory()))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dre.setCustosVariaveis(variaveis);

        BigDecimal fixos = financeService.listAllPayables().stream()
                .filter(p -> "paid".equals(p.getStatus()) && "fixed".equals(p.getCategory()))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dre.setCustosFixos(fixos);

        // 3. Cálculos de Resultados e Margens
        dre.setMargemContribuicao(receitas.subtract(variaveis));
        dre.setResultadoLiquido(dre.getMargemContribuicao().subtract(fixos));

        model.addAttribute("currentPage", "dre");
        model.addAttribute("dre", dre);

        return "dre";
    }
}