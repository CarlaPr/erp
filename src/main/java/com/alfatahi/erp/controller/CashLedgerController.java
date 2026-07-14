package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.CashLedgerEntryDto;
import com.alfatahi.erp.service.CashLedgerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/cash-ledger")
public class CashLedgerController {

    private final CashLedgerService cashLedgerService;

    public CashLedgerController(CashLedgerService cashLedgerService) {
        this.cashLedgerService = cashLedgerService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to   == null) to   = LocalDate.now();

        BigDecimal openingBalance = cashLedgerService.getOpeningBalance();
        List<CashLedgerEntryDto> entries = cashLedgerService.buildLedger(from, to, openingBalance);

        BigDecimal totalEntradas = entries.stream()
                .map(CashLedgerEntryDto::getEntrada)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaidas = entries.stream()
                .map(CashLedgerEntryDto::getSaida)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoFinal = entries.isEmpty()
                ? openingBalance
                : entries.get(entries.size() - 1).getSaldo();
        BigDecimal totalTaxas = entries.stream()
                .filter(CashLedgerEntryDto::isFinancialExpense)
                .map(CashLedgerEntryDto::getSaida)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentPage",    "cash-ledger");
        model.addAttribute("entries",        entries);
        model.addAttribute("from",           from);
        model.addAttribute("to",             to);
        model.addAttribute("openingBalance", openingBalance);
        model.addAttribute("totalEntradas",  totalEntradas);
        model.addAttribute("totalSaidas",    totalSaidas);
        model.addAttribute("saldoFinal",     saldoFinal);
        model.addAttribute("totalTaxas",     totalTaxas);
        return "cash-ledger";
    }
}
