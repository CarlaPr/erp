package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.CashLedgerEntryDto;
import com.alfatahi.erp.entity.FinancialMovement;
import com.alfatahi.erp.repository.FinancialMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CashLedgerService {

    private final FinancialMovementRepository movementRepository;

    public CashLedgerService(FinancialMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    public record BalanceSummary(BigDecimal total, BigDecimal bank, BigDecimal cash) { }

    @Transactional(readOnly = true)
    public List<CashLedgerEntryDto> buildLedger(LocalDate from, LocalDate to,
                                                  BigDecimal openingBalance) {
        List<CashLedgerEntryDto> entries = new ArrayList<>();
        for (FinancialMovement movement :
                movementRepository.findAllByMovementDateBetweenOrderByMovementDateAscCreatedAtAsc(from, to)) {
            if (!isActive(movement)) continue;
            entries.add(toLedgerEntry(movement));
        }

        BigDecimal balance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        for (CashLedgerEntryDto entry : entries) {
            balance = balance.add(entry.getEntrada()).subtract(entry.getSaida());
            entry.setSaldo(balance);
        }
        return entries;
    }

    @Transactional(readOnly = true)
    public BalanceSummary getOpeningBalances(LocalDate from) {
        return summarize(movementRepository.findAllByMovementDateBefore(from));
    }

    @Transactional(readOnly = true)
    public BalanceSummary getCurrentBalances() {
        return summarize(movementRepository.findAllByMovementDateBefore(LocalDate.now().plusDays(1)));
    }

    private BalanceSummary summarize(List<FinancialMovement> movements) {
        BigDecimal bank = BigDecimal.ZERO;
        BigDecimal cash = BigDecimal.ZERO;

        for (FinancialMovement movement : movements) {
            if (!isActive(movement)) continue;
            BigDecimal signedAmount = movement.getType() == FinancialMovement.MovementType.ENTRADA
                    ? movement.getAmount() : movement.getAmount().negate();
            if (movement.getBalanceLocation() == FinancialMovement.BalanceLocation.CASH) {
                cash = cash.add(signedAmount);
            } else {
                bank = bank.add(signedAmount);
            }
        }
        return new BalanceSummary(bank.add(cash), bank, cash);
    }

    private CashLedgerEntryDto toLedgerEntry(FinancialMovement movement) {
        CashLedgerEntryDto entry = new CashLedgerEntryDto();
        entry.setDate(movement.getMovementDate());
        entry.setType(movement.getType() == FinancialMovement.MovementType.ENTRADA
                ? CashLedgerEntryDto.EntryType.ENTRADA : CashLedgerEntryDto.EntryType.SAIDA);
        entry.setBalanceLocation(movement.getBalanceLocation() == FinancialMovement.BalanceLocation.CASH
                ? CashLedgerEntryDto.BalanceLocation.DINHEIRO : CashLedgerEntryDto.BalanceLocation.BANCO);
        entry.setPaymentMethod(movement.getPaymentMethod());

        if (movement.getAccountsReceivable() != null) {
            var receivable = movement.getAccountsReceivable();
            entry.setParty(receivable.getClient() != null ? receivable.getClient().getName() : "Cliente Avulso");
            entry.setDescription(receivable.getDescription());
            entry.setWorkOrderNumber(receivable.getWorkOrder() != null ? receivable.getWorkOrder().getNumber() : "—");
            entry.setEntrada(movement.getAmount());
            entry.setOrigin("Recebimento");
            entry.setNotes(receivable.getNotes());
        } else {
            var payable = movement.getAccountsPayable();
            boolean financialExpense = Boolean.TRUE.equals(payable.getFinancialExpense());
            entry.setParty(payable.getSupplier() != null ? payable.getSupplier().getName()
                    : (financialExpense ? "Taxa/Banco" : "Despesa Interna"));
            entry.setDescription(payable.getDescription());
            entry.setWorkOrderNumber(payable.getWorkOrder() != null ? payable.getWorkOrder().getNumber() : "—");
            entry.setSaida(movement.getAmount());
            entry.setFinancialExpense(financialExpense);
            entry.setOrigin(financialExpense ? "Taxa Cartão" : "Pagamento");
            entry.setNotes(payable.getNotes());
        }
        return entry;
    }

    private boolean isActive(FinancialMovement movement) {
        if (movement.getAccountsReceivable() != null) {
            String status = movement.getAccountsReceivable().getStatus();
            return "received".equals(status) || "partial".equals(status);
        }
        if (movement.getAccountsPayable() != null) {
            String status = movement.getAccountsPayable().getStatus();
            return "paid".equals(status) || "partial".equals(status);
        }
        return false;
    }
}
