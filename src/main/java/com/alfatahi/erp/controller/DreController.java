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
            @RequestParam(defaultValue = "comparative") String type,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        LocalDate now = LocalDate.now();

        if (year == null)
            year = now.getYear();

        if (month == null)
            month = now.getMonthValue();

        List<AccountsReceivable> receivables = recRepo.findAll();
        List<AccountsPayable> payables = payRepo.findAll();

        List<DreReportDto> colunas = new ArrayList<>();

        DateTimeFormatter fmt =
                DateTimeFormatter.ofPattern(
                        "MMM / yyyy",
                        new Locale("pt", "BR")
                );

        if ("annual".equals(type)) {

            DreReportDto coluna =
                    new DreReportDto("ANUAL " + year);

            for (AccountsReceivable r : receivables) {

                if (r.getDueDate() != null
                        && r.getDueDate().getYear() == year
                        && ("received".equals(r.getStatus())
                        || "partial".equals(r.getStatus()))) {

                    coluna.addReceita(
                            Optional.ofNullable(
                                    r.getReceivedAmount()
                            ).orElse(BigDecimal.ZERO)
                    );
                }
            }

            for (AccountsPayable p : payables) {

                if (p.getDueDate() != null
                        && p.getDueDate().getYear() == year) {

                    BigDecimal valor =
                            Optional.ofNullable(
                                    p.getTotalAmount()
                            ).orElse(BigDecimal.ZERO);

                    if ("variable".equalsIgnoreCase(
                            p.getCategory())) {

                        coluna.addCmv(valor);

                    } else {

                        coluna.addDespesa(valor);
                    }
                }
            }

            colunas.add(coluna);

        } else {

            int quantidade =
                    "single".equals(type) ? 1 : 4;

            LocalDate base =
                    LocalDate.of(year, month, 1);

            for (int i = quantidade - 1; i >= 0; i--) {

                LocalDate target =
                        quantidade == 1
                                ? base
                                : base.minusMonths(i);

                DreReportDto coluna =
                        new DreReportDto(
                                target.format(fmt)
                        );

                for (AccountsReceivable r : receivables) {

                    if (r.getDueDate() != null
                            && r.getDueDate().getMonth()
                            == target.getMonth()
                            && r.getDueDate().getYear()
                            == target.getYear()
                            && ("received".equals(
                            r.getStatus())
                            || "partial".equals(
                            r.getStatus()))) {

                        coluna.addReceita(
                                Optional.ofNullable(
                                        r.getReceivedAmount()
                                ).orElse(
                                        BigDecimal.ZERO
                                )
                        );
                    }
                }

                for (AccountsPayable p : payables) {

                    if (p.getDueDate() != null
                            && p.getDueDate().getMonth()
                            == target.getMonth()
                            && p.getDueDate().getYear()
                            == target.getYear()) {

                        BigDecimal valor =
                                Optional.ofNullable(
                                        p.getTotalAmount()
                                ).orElse(
                                        BigDecimal.ZERO
                                );

                        if ("variable".equalsIgnoreCase(
                                p.getCategory())) {

                            coluna.addCmv(valor);

                        } else {

                            coluna.addDespesa(valor);
                        }
                    }
                }

                colunas.add(coluna);
            }
        }

        model.addAttribute(
                "currentPage",
                "dre"
        );

        model.addAttribute(
                "dreColumns",
                colunas
        );

        model.addAttribute(
                "selectedType",
                type
        );

        model.addAttribute(
                "selectedMonth",
                month
        );

        model.addAttribute(
                "selectedYear",
                year
        );

        return "dre";
    }
}