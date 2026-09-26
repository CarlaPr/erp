package com.alfatahi.erp.controller;

import com.alfatahi.erp.service.FinancialPeriod;

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
            @RequestParam(required = false) String month,
            Model model) {
        FinancialPeriod period = FinancialPeriod.select(month, false, from, to);
        if (period.from() == null || period.to() == null) {
            period = new FinancialPeriod(null,
                    period.from() != null ? period.from() : FinancialPeriod.current().from(),
                    period.to() != null ? period.to() : FinancialPeriod.current().to());
        }
        period.addTo(model);
        from = period.from();
        to = period.to();

        CashLedgerService.BalanceSummary openingBalances = cashLedgerService.getOpeningBalances(from);
        BigDecimal openingBalance = openingBalances.total();
        List<CashLedgerEntryDto> entries = cashLedgerService.buildLedger(from, to, openingBalance);
        CashLedgerService.BalanceSummary currentBalances = cashLedgerService.getOpeningBalances(to.plusDays(1));

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
        model.addAttribute("saldoTotalAtual", currentBalances.total());
        model.addAttribute("saldoBancoAtual", currentBalances.bank());
        model.addAttribute("saldoDinheiroAtual", currentBalances.cash());
        model.addAttribute("openingBankBalance", openingBalances.bank());
        model.addAttribute("openingCashBalance", openingBalances.cash());
        return "cash-ledger";
    }
}
