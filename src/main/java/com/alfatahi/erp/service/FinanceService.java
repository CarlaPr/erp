package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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

    public List<AccountsPayable> listAllPayables() { return payableRepository.findAllByOrderByDueDateAsc(); }
    public List<AccountsReceivable> listAllReceivables() { return receivableRepository.findAllByOrderByDueDateAsc(); }
    public List<BankAccount> listAllAccounts() { return bankAccountRepository.findAll(); }
    public AccountsPayable savePayable(AccountsPayable p) { return payableRepository.save(p); }
    public AccountsReceivable saveReceivable(AccountsReceivable r) { return receivableRepository.save(r); }
    public BankAccount saveAccount(BankAccount a) { return bankAccountRepository.save(a); }

    // CORREÇÃO (bug #5): estes métodos não são usados em nenhuma tela hoje, mas
    // tinham o mesmo bug do dashboard (#4): somavam totalAmount só de status "paid"/
    // "received", ignorando recebimentos/pagamentos parciais. Corrigido para refletir
    // o valor efetivamente movimentado (receivedAmount/paidAmount), igual ao critério
    // já usado pela tela de Contas a Pagar.
    public BigDecimal getTotalReceivables() {
        return receivableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(r -> "received".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                // Valor líquido: bruto gravado - taxa de maquininha
                .map(AccountsReceivable::getNetReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPayables() {
        return payableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(p -> "paid".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                .map(AccountsPayable::getPaidAmount)
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


    @Transactional
    public void processPayablePayment(UUID payableId, BigDecimal amountPaid,
                                      LocalDate paymentDate, String paymentMethod, String notes) {
        AccountsPayable ap = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + payableId));

        BigDecimal newTotalPaid = ap.getPaidAmount().add(amountPaid);
        ap.setPaidAmount(newTotalPaid);

        if (paymentDate != null) {
            ap.setPaymentDate(paymentDate);
        }
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            ap.setPaymentMethod(paymentMethod);
        }
        if (notes != null && !notes.isBlank()) {
            ap.setNotes(notes);
        }

        if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0
                && newTotalPaid.compareTo(ap.getTotalAmount()) < 0) {
            ap.setStatus("partial");
        } else if (newTotalPaid.compareTo(ap.getTotalAmount()) >= 0) {
            ap.setStatus("paid");
        }
        payableRepository.save(ap);
    }

    @Transactional
    public void cancelPayable(UUID payableId) {
        AccountsPayable ap = payableRepository.findById(payableId).orElseThrow();
        ap.setStatus("cancelled"); // Regra: Não excluir fisicamente
        payableRepository.save(ap);
    }

    @Transactional
    public void processReceivablePayment(UUID receivableId, BigDecimal amountReceived,
                                         LocalDate paymentDate, BigDecimal cardFeePercent, String notes) {
        AccountsReceivable ar = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + receivableId));

        // REGRA DE NEGÓCIO (A2): a taxa de cartão é informada no momento do
        // recebimento (ela não é fixa — o banco pode mudar a taxa a qualquer mês).
        // "amountReceived" é o valor líquido que de fato chegou (ex.: o que aparece
        // no extrato). A partir da taxa informada, calculamos o valor bruto
        // equivalente, e é esse valor bruto que abate o saldo da conta a receber.
        // Assim, uma venda de R$10.000 com 5% de taxa, recebendo R$9.500 líquidos,
        // fecha a conta (saldo zero) em vez de ficar "parcial" para sempre — que era
        // o bug #9 da auditoria.
        BigDecimal fee = (cardFeePercent != null) ? cardFeePercent : BigDecimal.ZERO;
        BigDecimal grossAmount;
        BigDecimal feeForThisPayment;

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            if (fee.compareTo(new BigDecimal("100")) >= 0) {
                throw new IllegalArgumentException("Taxa de cartão inválida: deve ser menor que 100%.");
            }
            BigDecimal divisor = BigDecimal.ONE.subtract(fee.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
            grossAmount = amountReceived.divide(divisor, 2, RoundingMode.HALF_UP);
            feeForThisPayment = grossAmount.subtract(amountReceived);
        } else {
            grossAmount = amountReceived;
            feeForThisPayment = BigDecimal.ZERO;
        }

        BigDecimal current = ar.getReceivedAmount() != null ? ar.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal newTotalReceived = current.add(grossAmount);

        ar.setReceivedAmount(newTotalReceived);
        ar.setFeeAmount(ar.getFeeAmount().add(feeForThisPayment));

        if (paymentDate != null) {
            ar.setPaymentDate(paymentDate);
        }
        if (notes != null && !notes.isBlank()) {
            ar.setNotes(notes);
        }
        if (feeForThisPayment.compareTo(BigDecimal.ZERO) > 0) {
            String autoNote = String.format(
                    "[Taxa cartão %.2f%% sobre este recebimento = R$ %.2f | líquido informado R$ %.2f | valor bruto liquidado R$ %.2f]",
                    fee, feeForThisPayment, amountReceived, grossAmount);
            String existing = ar.getNotes();
            ar.setNotes((existing != null && !existing.isBlank() ? existing + " " : "") + autoNote);
        }

        if (newTotalReceived.compareTo(BigDecimal.ZERO) > 0
                && newTotalReceived.compareTo(ar.getTotalAmount()) < 0) {
            ar.setStatus("partial");
        } else if (newTotalReceived.compareTo(ar.getTotalAmount()) >= 0) {
            ar.setStatus("received");
        }
        receivableRepository.save(ar);
    }

    @Transactional
    public void cancelReceivable(UUID receivableId) {
        AccountsReceivable ar = receivableRepository.findById(receivableId).orElseThrow();
        ar.setStatus("cancelled"); // Remove automaticamente de DRE/Fluxo por causa do status
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