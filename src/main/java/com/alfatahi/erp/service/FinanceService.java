package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private final AccountsPayableRepository payableRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final BankAccountRepository bankAccountRepository;

    public FinanceService(AccountsPayableRepository payableRepository, AccountsReceivableRepository receivableRepository, BankAccountRepository bankAccountRepository) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<AccountsPayable> listAllPayables() { return payableRepository.findAllByOrderByDueDateAsc(); }
    public List<AccountsReceivable> listAllReceivables() { return receivableRepository.findAllByOrderByDueDateAsc(); }
    public List<BankAccount> listAllAccounts() { return bankAccountRepository.findAll(); }

    public AccountsPayable savePayable(AccountsPayable p) { return payableRepository.save(p); }
    public AccountsReceivable saveReceivable(AccountsReceivable r) { return receivableRepository.save(r); }
    public BankAccount saveAccount(BankAccount a) { return bankAccountRepository.save(a); }

    public BigDecimal getTotalReceivables() {
        return receivableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(r -> "received".equals(r.getStatus()))
                .map(AccountsReceivable::getTotalAmount) // CORRIGIDO: alterado de . para ::
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPayables() {
        return payableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(p -> "paid".equals(p.getStatus()))
                .map(AccountsPayable::getTotalAmount) // CORRIGIDO: alterado de . para ::
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void createDefaultAccountIfEmpty() {
        if (bankAccountRepository.count() == 0) {
            BankAccount defaultAcc = new BankAccount();
            defaultAcc.setName("Caixa Interno Geral");
            defaultAcc.setBankName("Dinheiro / Caixa");
            defaultAcc.setType("cash");
            defaultAcc.setCurrentBalance(new BigDecimal("5000.00")); // Saldo simulado inicial
            bankAccountRepository.save(defaultAcc);
        }
    }
}