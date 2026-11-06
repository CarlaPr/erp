package com.alfatahi.erp.dto;

import com.alfatahi.erp.service.CashLedgerService.BalanceSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CashLedgerReport(LocalDate from, LocalDate to, List<CashLedgerEntryDto> entries,
                               BalanceSummary openingBalances, BalanceSummary closingBalances,
                               BigDecimal totalEntradas, BigDecimal totalSaidas, BigDecimal totalTaxas) {
    public BigDecimal openingBalance() {
        return openingBalances.total();
    }

    public BigDecimal saldoFinal() {
        return closingBalances.total();
    }
}
