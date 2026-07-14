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

/**
 * Gerencia o fechamento financeiro mensal da vidraçaria.
 *
 * Regras de negócio:
 *  - O período de fechamento vai do dia 01 ao dia 06 de cada mês (dia 6 é o dia de fechamento).
 *  - O fechamento só pode ser executado a partir do dia 6 do mês corrente.
 *  - O saldo final (closing_balance) vira automaticamente o saldo inicial (opening_balance)
 *    do próximo fechamento — carry-forward real.
 *  - Nunca sobrescreve um fechamento já existente para o mesmo período.
 */
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

    /**
     * Executa o fechamento do período que cobre do dia 1 ao dia 6 do mês indicado.
     *
     * @param year  ano do período
     * @param month mês do período (1-12)
     * @param closedBy usuário/login que executou o fechamento
     * @param notes observações opcionais
     */
    @Transactional
    public FinancialClosing executeClosing(int year, int month, String closedBy, String notes) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd   = LocalDate.of(year, month, 6);   // inclusivo

        // Janela de execução: só pode fechar a partir do dia 6
        if (LocalDate.now().isBefore(periodEnd)) {
            throw new IllegalStateException(
                    "O fechamento só pode ser executado a partir do dia 6 do mês corrente.");
        }

        // Impede fechamento duplicado do mesmo período
        if (closingRepo.findByPeriodStart(periodStart).isPresent()) {
            throw new IllegalStateException(
                    "Já existe um fechamento para o período " + periodStart + ".");
        }

        // Saldo de abertura = saldo final do último fechamento (ou zero)
        BigDecimal openingBalance = closingRepo.findLatestClosing()
                .map(FinancialClosing::getClosingBalance)
                .orElse(BigDecimal.ZERO);

        // Intervalo de datas para as queries: do dia 1 ao dia 6 (exclusive dia 7)
        LocalDate queryStart = periodStart;
        LocalDate queryEnd   = periodEnd.plusDays(1);  // exclusive para BETWEEN nas queries

        BigDecimal totalIn      = nvl(receivableRepo.sumEntradasRealByPeriod(queryStart, queryEnd));
        BigDecimal totalOut     = nvl(payableRepo.sumSaidasRealByPeriod(queryStart, queryEnd));
        BigDecimal financialExp = nvl(payableRepo.sumDespesasFinanceirasByMonthAndYear(queryStart, queryEnd));
        BigDecimal pending      = nvl(receivableRepo.sumPendentesByPeriod(queryStart, queryEnd));
        BigDecimal received     = totalIn;

        // Net profit no período = entradas - saídas (incluindo despesas financeiras)
        BigDecimal netProfit = totalIn.subtract(totalOut);

        // Saldo final = saldo inicial + entradas - saídas
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
