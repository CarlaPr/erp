package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.DreReportDto;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class DreController {

    private final AccountsReceivableRepository recRepo;
    private final AccountsPayableRepository payRepo;

    public DreController(AccountsReceivableRepository recRepo, AccountsPayableRepository payRepo) {
        this.recRepo = recRepo;
        this.payRepo = payRepo;
    }

    @GetMapping("/dre")
    public String generateDre(
            @RequestParam(defaultValue = "comparative") String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        List<DreReportDto> colunas = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM / yyyy", new Locale("pt", "BR"));

        if ("annual".equals(type)) {
            DreReportDto coluna = new DreReportDto("ANUAL " + year);

            // Loop eficiente (chamando o banco mês a mês para poupar RAM)
            for (int m = 1; m <= 12; m++) {
                BigDecimal receita = recRepo.sumReceivedByMonthAndYear(m, year);
                BigDecimal cmv = payRepo.sumCmvByMonthAndYear(m, year);
                BigDecimal fixa = payRepo.sumDespesasFixasByMonthAndYear(m, year);

                coluna.addReceita(receita != null ? receita : BigDecimal.ZERO);
                coluna.addCmv(cmv != null ? cmv : BigDecimal.ZERO);
                coluna.addDespesa(fixa != null ? fixa : BigDecimal.ZERO);
            }
            colunas.add(coluna);
        } else {
            int quantidade = "single".equals(type) ? 1 : 4;
            LocalDate base = LocalDate.of(year, month, 1);

            for (int i = quantidade - 1; i >= 0; i--) {
                LocalDate target = quantidade == 1 ? base : base.minusMonths(i);
                DreReportDto coluna = new DreReportDto(target.format(fmt));

                BigDecimal receita = recRepo.sumReceivedByMonthAndYear(target.getMonthValue(), target.getYear());
                BigDecimal cmv = payRepo.sumCmvByMonthAndYear(target.getMonthValue(), target.getYear());
                BigDecimal fixa = payRepo.sumDespesasFixasByMonthAndYear(target.getMonthValue(), target.getYear());

                coluna.addReceita(receita != null ? receita : BigDecimal.ZERO);
                coluna.addCmv(cmv != null ? cmv : BigDecimal.ZERO);
                coluna.addDespesa(fixa != null ? fixa : BigDecimal.ZERO);
                colunas.add(coluna);
            }
        }

        model.addAttribute("currentPage", "dre");
        model.addAttribute("dreColumns", colunas);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);

        return "dre";
    }
}