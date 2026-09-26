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


    private static final int DIA_INICIO_PERIODO = FinancialPeriod.START_DAY;



    private final FinancialClosingRepository closingRepo;
    private final CashLedgerService cashLedgerService;
    private final AccountsReceivableRepository receivableRepo;
    private final AccountsPayableRepository payableRepo;

    public FinancialClosingService(FinancialClosingRepository closingRepo,
                                    AccountsReceivableRepository receivableRepo,
                                    AccountsPayableRepository payableRepo, CashLedgerService cashLedgerService) {
        this.closingRepo   = closingRepo;
        this.cashLedgerService = cashLedgerService;
        this.receivableRepo = receivableRepo;
        this.payableRepo    = payableRepo;
    }

    public List<FinancialClosing> listAll() {
        return closingRepo.findAllByOrderByPeriodStartDesc();
    }


    public LocalDate periodoFim(LocalDate periodStart) {
        return FinancialPeriod.monthly(java.time.YearMonth.from(periodStart)).to();
    }


    public LocalDate dataLiberacaoFechamento(LocalDate periodStart) {
        return periodoFim(periodStart).plusDays(1);
    }

    public boolean periodoLiberadoParaFechamento(LocalDate periodStart) {
        return !FinancialPeriod.today().isBefore(dataLiberacaoFechamento(periodStart));
    }


    public LocalDate proximoPeriodoParaFechar() {
        return closingRepo.findLatestClosing()
                .map(fc -> fc.getPeriodStart().plusMonths(1))
                .orElse(FinancialPeriod.current().from().minusMonths(1));
    }

    @Transactional
    public FinancialClosing executeClosing(int year, int month, String closedBy, String notes) {
        LocalDate periodStart = LocalDate.of(year, month, DIA_INICIO_PERIODO);

        LocalDate periodEnd   = periodoFim(periodStart);

        LocalDate dataLiberacao = dataLiberacaoFechamento(periodStart);
        if (FinancialPeriod.today().isBefore(dataLiberacao)) {
            throw new IllegalStateException(
                    "O fechamento do período de " + periodStart + " a " + periodEnd
                            + " só pode ser executado a partir de " + dataLiberacao + ".");
        }

        if (closingRepo.findByPeriodStart(periodStart).isPresent()) {
            throw new IllegalStateException(
                    "Já existe um fechamento para o período " + periodStart + ".");
        }



        closingRepo.findLatestClosing().ifPresent(ultimo -> {
            LocalDate esperado = ultimo.getPeriodStart().plusMonths(1);
            if (!periodStart.equals(esperado)) {
                throw new IllegalStateException(
                        "Os fechamentos devem ser feitos em ordem. O próximo período pendente é "
                                + esperado + ".");
            }
        });

        BigDecimal openingBalance = closingRepo.findLatestClosing()
                .map(FinancialClosing::getClosingBalance)
                .orElseGet(() -> cashLedgerService.getOpeningBalances(periodStart).total());

        LocalDate queryStart = periodStart;
        LocalDate queryEnd   = periodEnd.plusDays(1);

        CashLedgerService.PeriodSummary cash = cashLedgerService.getPeriodSummary(queryStart, periodEnd);
        BigDecimal totalIn = cash.in();
        BigDecimal totalOut = cash.out();



        BigDecimal financialExp = cash.financialExpenses();
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


    @Transactional
    public void reabrirUltimoFechamento() {
        FinancialClosing ultimo = closingRepo.findLatestClosing()
                .orElseThrow(() -> new IllegalStateException("Não há nenhum fechamento para reabrir."));
        closingRepo.delete(ultimo);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
