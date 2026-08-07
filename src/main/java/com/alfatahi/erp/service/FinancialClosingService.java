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

    /** Dia do mês em que o período de fechamento tem início. */
    private static final int DIA_INICIO_PERIODO = 6;

    /** Dia do mês seguinte em que o período de fechamento termina. */
    private static final int DIA_FIM_PERIODO = 5;

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
     * Data final do período iniciado em {@code periodStart} (dia 6 de um mês):
     * dia 5 do mês seguinte.
     *
     * IMPORTANTE: o período vai do dia 6 ATÉ o dia 5 do mês seguinte (e não o contrário)
     * exatamente para que o próximo período comece no dia seguinte (dia 6) sem sobreposição.
     * Se fosse "dia 5 até dia 6 do mês seguinte", dois fechamentos consecutivos passariam
     * a contar os dias 5 e 6 do mês de transição EM DOBRO (em ambos os fechamentos).
     */
    public LocalDate periodoFim(LocalDate periodStart) {
        return periodStart.plusMonths(1).withDayOfMonth(DIA_FIM_PERIODO);
    }

    /** Data a partir da qual o período pode ser fechado: o dia seguinte ao término do período. */
    public LocalDate dataLiberacaoFechamento(LocalDate periodStart) {
        return periodoFim(periodStart).plusDays(1);
    }

    public boolean periodoLiberadoParaFechamento(LocalDate periodStart) {
        return !LocalDate.now().isBefore(dataLiberacaoFechamento(periodStart));
    }

    /**
     * Próximo período (dia 6 de um mês) ainda não fechado, em sequência ao último fechamento existente.
     * Se nunca houve fechamento, sugere o mês anterior ao atual como ponto de partida.
     */
    public LocalDate proximoPeriodoParaFechar() {
        return closingRepo.findLatestClosing()
                .map(fc -> fc.getPeriodStart().plusMonths(1))
                .orElse(LocalDate.now().withDayOfMonth(DIA_INICIO_PERIODO).minusMonths(1));
    }

    @Transactional
    public FinancialClosing executeClosing(int year, int month, String closedBy, String notes) {
        LocalDate periodStart = LocalDate.of(year, month, DIA_INICIO_PERIODO);
        // Período de apuração: do dia 6 do mês selecionado até o dia 5 do mês seguinte (sem sobreposição).
        LocalDate periodEnd   = periodoFim(periodStart);

        LocalDate dataLiberacao = dataLiberacaoFechamento(periodStart);
        if (LocalDate.now().isBefore(dataLiberacao)) {
            throw new IllegalStateException(
                    "O fechamento do período de " + periodStart + " a " + periodEnd
                            + " só pode ser executado a partir de " + dataLiberacao + ".");
        }

        if (closingRepo.findByPeriodStart(periodStart).isPresent()) {
            throw new IllegalStateException(
                    "Já existe um fechamento para o período " + periodStart + ".");
        }

        // Os fechamentos devem seguir a ordem cronológica, sem pular meses, para que o
        // saldo de abertura de cada período corresponda de fato ao saldo de fechamento
        // do período imediatamente anterior.
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
                .orElse(BigDecimal.ZERO);

        LocalDate queryStart = periodStart;
        LocalDate queryEnd   = periodEnd.plusDays(1);

        BigDecimal totalIn      = nvl(receivableRepo.sumEntradasRealByPeriod(queryStart, queryEnd));
        BigDecimal totalOut     = nvl(payableRepo.sumSaidasRealByPeriod(queryStart, queryEnd));
        // Regime de CAIXA (data de pagamento), consistente com totalIn/totalOut acima —
        // e não sumDespesasFinanceirasByMonthAndYear, que é por data de vencimento (regime de
        // competência, usado na DRE) e por isso não deve ser usado num fechamento de caixa.
        BigDecimal financialExp = nvl(payableRepo.sumDespesasFinanceirasRecebidas(queryStart, queryEnd));
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

    /**
     * Reabre (remove) apenas o fechamento MAIS RECENTE, para permitir refazê-lo caso tenha sido
     * calculado com regras antigas/incorretas. Não é permitido reabrir um fechamento no meio da
     * sequência, pois isso quebraria o encadeamento de saldo de abertura/fechamento dos demais.
     */
    @Transactional
    public void reabrirUltimoFechamento() {
        FinancialClosing ultimo = closingRepo.findLatestClosing()
                .orElseThrow(() -> new IllegalStateException("Não há nenhum fechamento para reabrir."));
        closingRepo.delete(ultimo);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
