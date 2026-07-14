package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.CashLedgerEntryDto;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.FinancialClosingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Monta o Livro Caixa real: entradas (recebimentos líquidos) e saídas (pagamentos)
 * ordenados cronologicamente, com o saldo acumulado recalculado após cada movimento.
 *
 * NÃO cria nova tabela — combina os dados já existentes em AccountsReceivable e
 * AccountsPayable para evitar duplicidade de estado.
 */
@Service
public class CashLedgerService {

    private final AccountsReceivableRepository receivableRepo;
    private final AccountsPayableRepository payableRepo;
    private final FinancialClosingRepository closingRepo;

    public CashLedgerService(AccountsReceivableRepository receivableRepo,
                              AccountsPayableRepository payableRepo,
                              FinancialClosingRepository closingRepo) {
        this.receivableRepo = receivableRepo;
        this.payableRepo    = payableRepo;
        this.closingRepo    = closingRepo;
    }

    /**
     * Retorna o Livro Caixa para o período informado.
     *
     * @param from  início (inclusive)
     * @param to    fim (inclusive)
     * @param openingBalance saldo de abertura do período (vindo do fechamento anterior)
     */
    @Transactional(readOnly = true)
    public List<CashLedgerEntryDto> buildLedger(LocalDate from, LocalDate to,
                                                  BigDecimal openingBalance) {
        List<CashLedgerEntryDto> entries = new ArrayList<>();

        // ── Entradas: recebimentos líquidos efetivados ────────────────────────
        for (AccountsReceivable ar : receivableRepo.findAll()) {
            if (!"received".equals(ar.getStatus()) && !"partial".equals(ar.getStatus())) continue;
            LocalDate pd = ar.getPaymentDate();
            if (pd == null || pd.isBefore(from) || pd.isAfter(to)) continue;
            if (ar.getReceivedAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

            CashLedgerEntryDto e = new CashLedgerEntryDto();
            e.setDate(pd);
            e.setType(CashLedgerEntryDto.EntryType.ENTRADA);
            e.setParty(ar.getClient() != null ? ar.getClient().getName() : "Cliente Avulso");
            e.setDescription(ar.getDescription());
            e.setWorkOrderNumber(ar.getWorkOrder() != null ? ar.getWorkOrder().getNumber() : "—");
            e.setPaymentMethod(ar.getPaymentMethod());
            e.setEntrada(ar.getReceivedAmount());   // valor LÍQUIDO no caixa
            e.setOrigin("Recebimento");
            e.setNotes(ar.getNotes());
            entries.add(e);
        }

        // ── Saídas: pagamentos efetivados (incluindo taxas de cartão) ─────────
        for (AccountsPayable ap : payableRepo.findAll()) {
            if (!"paid".equals(ap.getStatus()) && !"partial".equals(ap.getStatus())) continue;
            LocalDate pd = ap.getPaymentDate();
            if (pd == null || pd.isBefore(from) || pd.isAfter(to)) continue;
            if (ap.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) continue;

            CashLedgerEntryDto e = new CashLedgerEntryDto();
            e.setDate(pd);
            e.setType(CashLedgerEntryDto.EntryType.SAIDA);
            e.setParty(ap.getSupplier() != null ? ap.getSupplier().getName()
                    : (Boolean.TRUE.equals(ap.getFinancialExpense()) ? "Taxa/Banco" : "Despesa Interna"));
            e.setDescription(ap.getDescription());
            e.setWorkOrderNumber(ap.getWorkOrder() != null ? ap.getWorkOrder().getNumber() : "—");
            e.setPaymentMethod(ap.getPaymentMethod());
            e.setSaida(ap.getPaidAmount());
            e.setFinancialExpense(Boolean.TRUE.equals(ap.getFinancialExpense()));
            e.setOrigin(Boolean.TRUE.equals(ap.getFinancialExpense()) ? "Taxa Cartão" : "Pagamento");
            e.setNotes(ap.getNotes());
            entries.add(e);
        }

        // ── Ordena por data e, em caso de empate, Entradas antes de Saídas ───
        entries.sort(Comparator.comparing(CashLedgerEntryDto::getDate)
                .thenComparing(e -> e.getType() == CashLedgerEntryDto.EntryType.ENTRADA ? 0 : 1));

        // ── Calcula saldo acumulado ───────────────────────────────────────────
        BigDecimal saldo = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        for (CashLedgerEntryDto e : entries) {
            saldo = saldo.add(e.getEntrada()).subtract(e.getSaida());
            e.setSaldo(saldo);
        }

        return entries;
    }

    /** Saldo de abertura: vem do último fechamento, ou zero se não houver nenhum. */
    public BigDecimal getOpeningBalance() {
        return closingRepo.findLatestClosing()
                .map(fc -> fc.getClosingBalance())
                .orElse(BigDecimal.ZERO);
    }
}
