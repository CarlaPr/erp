package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private final AccountsPayableRepository payableRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final BankAccountRepository bankAccountRepository;
    private final WorkOrderRepository workOrderRepository; // Adicionado para Rateio

    public FinanceService(AccountsPayableRepository payableRepository,
                          AccountsReceivableRepository receivableRepository,
                          BankAccountRepository bankAccountRepository,
                          WorkOrderRepository workOrderRepository) { // Construtor atualizado
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.workOrderRepository = workOrderRepository;
    }

    // --- MÉTODOS EXISTENTES (Mantidos intactos) ---
    public List<AccountsPayable> listAllPayables() { return payableRepository.findAllByOrderByDueDateAsc(); }
    public List<AccountsReceivable> listAllReceivables() { return receivableRepository.findAllByOrderByDueDateAsc(); }
    public List<BankAccount> listAllAccounts() { return bankAccountRepository.findAll(); }
    public AccountsPayable savePayable(AccountsPayable p) { return payableRepository.save(p); }
    public AccountsReceivable saveReceivable(AccountsReceivable r) { return receivableRepository.save(r); }
    public BankAccount saveAccount(BankAccount a) { return bankAccountRepository.save(a); }

    public BigDecimal getTotalReceivables() {
        return receivableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(r -> "received".equals(r.getStatus()))
                .map(AccountsReceivable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPayables() {
        return payableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(p -> "paid".equals(p.getStatus()))
                .map(AccountsPayable::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void createDefaultAccountIfEmpty() {
        if (bankAccountRepository.count() == 0) {
            BankAccount defaultAcc = new BankAccount();
            defaultAcc.setName("Caixa Interno Geral");
            defaultAcc.setBankName("Dinheiro / Caixa");
            defaultAcc.setType("cash");
            defaultAcc.setCurrentBalance(new BigDecimal("5000.00"));
            bankAccountRepository.save(defaultAcc);
        }
    }

    // --- NOVOS MÉTODOS DE REGRA DE NEGÓCIO ---

    @Transactional
    public void processReceivablePayment(UUID receivableId, BigDecimal amountReceived) {
        AccountsReceivable ar = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        BigDecimal newTotalReceived = ar.getReceivedAmount().add(amountReceived);
        ar.setReceivedAmount(newTotalReceived);

        // Regra de Status Automático
        if (newTotalReceived.compareTo(BigDecimal.ZERO) > 0 && newTotalReceived.compareTo(ar.getTotalAmount()) < 0) {
            ar.setStatus("partial");
        } else if (newTotalReceived.compareTo(ar.getTotalAmount()) >= 0) {
            ar.setStatus("received");
        }
        receivableRepository.save(ar);
    }

    public java.util.Map<String, java.math.BigDecimal> getExpensesBySubcategory() {
        return payableRepository.findAll().stream()
                // Filtra apenas contas que tenham subcategoria preenchida
                .filter(p -> p.getSubcategory() != null && !p.getSubcategory().trim().isEmpty())
                // Agrupa pelo nome da subcategoria e soma o valor total
                .collect(java.util.stream.Collectors.groupingBy(
                        AccountsPayable::getSubcategory,
                        java.util.stream.Collectors.reducing(
                                java.math.BigDecimal.ZERO,
                                AccountsPayable::getTotalAmount,
                                java.math.BigDecimal::add
                        )
                ));
    }

    @Transactional
    public void processPayablePayment(UUID payableId, BigDecimal amountPaid) {
        AccountsPayable ap = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        BigDecimal newTotalPaid = ap.getPaidAmount().add(amountPaid);
        ap.setPaidAmount(newTotalPaid);

        // Regra de Status Automático
        if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0 && newTotalPaid.compareTo(ap.getTotalAmount()) < 0) {
            ap.setStatus("partial");
        } else if (newTotalPaid.compareTo(ap.getTotalAmount()) >= 0) {
            ap.setStatus("paid");
        }
        payableRepository.save(ap);
    }

    @Transactional
    public void allocateExpenseToWorkOrder(UUID payableId, UUID workOrderId, BigDecimal percentage) {
        AccountsPayable ap = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new RuntimeException("OS não encontrada"));

        // Cálculo do valor rateado
        BigDecimal value = ap.getTotalAmount()
                .multiply(percentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        ExpenseAllocation allocation = new ExpenseAllocation();
        allocation.setAccountsPayable(ap);
        allocation.setWorkOrder(wo);
        allocation.setPercentage(percentage);
        allocation.setValue(value);

        ap.getAllocations().add(allocation);
        payableRepository.save(ap);
    }
}