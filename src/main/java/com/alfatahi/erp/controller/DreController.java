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
            @RequestParam(required = false, defaultValue = "comparative") String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        // Valores por defeito (Mês e Ano atuais)
        LocalDate now = LocalDate.now();
        if (year == null) year = now.getYear();
        if (month == null) month = now.getMonthValue();

        List<AccountsReceivable> receivables = recRepo.findAll();
        List<AccountsPayable> payables = payRepo.findAll();

        List<DreReportDto> colunas = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM / yyyy", new Locale("pt", "PT"));

        if ("annual".equals(type)) {
            // ==========================================
            // VISÃO ANUAL (1 Coluna com o total do ano)
            // ==========================================
            DreReportDto coluna = new DreReportDto("ANUAL " + year);

            for (AccountsReceivable r : receivables) {
                if (r.getDueDate() != null && r.getDueDate().getYear() == year) {
                    coluna.addReceita(r.getTotalAmount());
                }
            }
            for (AccountsPayable p : payables) {
                if (p.getDueDate() != null && p.getDueDate().getYear() == year) {
                    if ("variable".equals(p.getCategory())) coluna.addCmv(p.getTotalAmount());
                    else coluna.addDespesa(p.getTotalAmount());
                }
            }
            colunas.add(coluna);

        } else if ("single".equals(type)) {
            // ==========================================
            // VISÃO MENSAL ÚNICA (1 Coluna)
            // ==========================================
            LocalDate target = LocalDate.of(year, month, 1);
            DreReportDto coluna = new DreReportDto(target.format(fmt).toUpperCase());

            for (AccountsReceivable r : receivables) {
                if (r.getDueDate() != null && r.getDueDate().getMonthValue() == month && r.getDueDate().getYear() == year) {
                    coluna.addReceita(r.getTotalAmount());
                }
            }
            for (AccountsPayable p : payables) {
                if (p.getDueDate() != null && p.getDueDate().getMonthValue() == month && p.getDueDate().getYear() == year) {
                    if ("variable".equals(p.getCategory())) coluna.addCmv(p.getTotalAmount());
                    else coluna.addDespesa(p.getTotalAmount());
                }
            }
            colunas.add(coluna);

        } else {
            // ==========================================
            // VISÃO COMPARATIVA (4 Meses terminando no mês escolhido)
            // ==========================================
            LocalDate baseDate = LocalDate.of(year, month, 1);
            for (int i = 3; i >= 0; i--) {
                LocalDate target = baseDate.minusMonths(i);
                DreReportDto coluna = new DreReportDto(target.format(fmt).toUpperCase());

                for (AccountsReceivable r : receivables) {
                    if (r.getDueDate() != null && r.getDueDate().getMonth() == target.getMonth() && r.getDueDate().getYear() == target.getYear()) {
                        coluna.addReceita(r.getTotalAmount());
                    }
                }
                for (AccountsPayable p : payables) {
                    if (p.getDueDate() != null && p.getDueDate().getMonth() == target.getMonth() && p.getDueDate().getYear() == target.getYear()) {
                        if ("variable".equals(p.getCategory())) coluna.addCmv(p.getTotalAmount());
                        else coluna.addDespesa(p.getTotalAmount());
                    }
                }
                colunas.add(coluna);
            }
        }

        // Injetar dados no HTML
        model.addAttribute("currentPage", "dre");
        model.addAttribute("dreColumns", colunas);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedYear", year);

        return "dre";
    }
}