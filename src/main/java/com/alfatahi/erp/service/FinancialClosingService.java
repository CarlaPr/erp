package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.FinancialClosing;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.FinancialClosingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Service
public class FinancialClosingService {

    private final FinancialClosingRepository closingRepo;
    private final AccountsReceivableRepository receivableRepo;
    private final AccountsPayableRepository payableRepo;

    public FinancialClosingService(FinancialClosingRepository closingRepo,
                                    AccountsReceivableRepository receivableRepo,
                                    AccountsPayableRepository payableRepo) {
        this.closingRepo   = closingRepo;
        this.receivableRepo = receivableRepo;
        this.payableRepo    = payableRepo;
    }

    public List<FinancialClosing> listAll() {
        return closingRepo.findAllByOrderByPeriodStartDesc();
    }

    @Transactional
    public FinancialClosing executeClosing(int year, int month, String closedBy, String notes) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd   = LocalDate.of(year, month, 6);   // inclusivo

        if (LocalDate.now().isBefore(periodEnd)) {
            throw new IllegalStateException(
                    "O fechamento só pode ser executado a partir do dia 6 do mês corrente.");
        }

        if (closingRepo.findByPeriodStart(periodStart).isPresent()) {
            throw new IllegalStateException(
                    "Já existe um fechamento para o período " + periodStart + ".");
        }

        BigDecimal openingBalance = closingRepo.findLatestClosing()
                .map(FinancialClosing::getClosingBalance)
                .orElse(BigDecimal.ZERO);

        LocalDate queryStart = periodStart;
        LocalDate queryEnd   = periodEnd.plusDays(1);

        BigDecimal totalIn      = nvl(receivableRepo.sumEntradasRealByPeriod(queryStart, queryEnd));
        BigDecimal totalOut     = nvl(payableRepo.sumSaidasRealByPeriod(queryStart, queryEnd));
        BigDecimal financialExp = nvl(payableRepo.sumDespesasFinanceirasByMonthAndYear(queryStart, queryEnd));
        BigDecimal pending      = nvl(receivableRepo.sumPendentesByPeriod(queryStart, queryEnd));
        BigDecimal received     = totalIn;

        BigDecimal netProfit = totalIn.subtract(totalOut);

        BigDecimal closingBalance = openingBalance.add(totalIn).subtract(totalOut);

        FinancialClosing closing = new FinancialClosing();
        closing.setPeriodStart(periodStart);
        closing.setPeriodEnd(periodEnd);
        closing.setOpeningBalance(openingBalance);
        closing.setTotalIn(totalIn);
        closing.setTotalOut(totalOut);
        closing.setFinancialExpenses(financialExp);
        closing.setPendingAmount(pending);
        closing.setReceivedAmount(received);
        closing.setNetProfit(netProfit);
        closing.setClosingBalance(closingBalance);
        closing.setClosedBy(closedBy);
        closing.setNotes(notes);

        return closingRepo.save(closing);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
