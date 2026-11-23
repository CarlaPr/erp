package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.BankAccountBalance;
import com.alfatahi.erp.entity.BankAccount;
import com.alfatahi.erp.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class BankAccountService {

    private final BankAccountRepository repository;

    public BankAccountService(BankAccountRepository repository) {
        this.repository = repository;
    }

    public List<BankAccount> listAccounts() {
        return repository.findAllByOpeningBalanceIsNotNullOrderByNameAsc();
    }

    public List<BankAccountBalance> currentBalances() {
        return repository.findBalances(FinancialPeriod.today());
    }

    public BankAccount requireAccount(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Selecione a conta bancária da movimentação.");
        }
        return repository.findByIdAndOpeningBalanceIsNotNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta bancária não encontrada."));
    }

    public void create(String name, BigDecimal openingBalance) {
        String accountName = name.strip();
        if (accountName.isEmpty() || accountName.length() > 100) {
            throw new IllegalArgumentException("Informe um nome para a conta bancária com até 100 caracteres.");
        }
        if (openingBalance == null || openingBalance.stripTrailingZeros().scale() > 2
                || openingBalance.abs().compareTo(new BigDecimal("1000000000000")) >= 0) {
            throw new IllegalArgumentException("Informe um saldo inicial válido com até duas casas decimais.");
        }
        repository.save(new BankAccount(accountName, openingBalance));
    }
}
