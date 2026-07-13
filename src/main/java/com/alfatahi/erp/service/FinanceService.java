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
    private final WorkOrderRepository workOrderRepository;

    public FinanceService(AccountsPayableRepository payableRepository,
                          AccountsReceivableRepository receivableRepository,
                          BankAccountRepository bankAccountRepository,
                          WorkOrderRepository workOrderRepository) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.workOrderRepository = workOrderRepository;
    }

    public List<AccountsPayable> listAllPayables() { return payableRepository.findAllByOrderByDueDateAscCreatedAtAsc(); }
    public List<AccountsReceivable> listAllReceivables() { return receivableRepository.findAllByOrderByDueDateAsc(); }
    public List<BankAccount> listAllAccounts() { return bankAccountRepository.findAll(); }
    public AccountsPayable savePayable(AccountsPayable p) { return payableRepository.save(p); }
    public AccountsReceivable saveReceivable(AccountsReceivable r) { return receivableRepository.save(r); }
    public BankAccount saveAccount(BankAccount a) { return bankAccountRepository.save(a); }

    public BigDecimal getTotalReceivables() {
        return receivableRepository.sumTotalReceivables();
    }

    public BigDecimal getTotalPayables() {
        return payableRepository.sumTotalPayables();
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
        ap.setStatus("cancelled");
        payableRepository.save(ap);
    }

    @Transactional
    public void processReceivablePayment(UUID receivableId, BigDecimal amountReceived,
                                         LocalDate paymentDate, BigDecimal cardFeePercent, String notes) {
        AccountsReceivable ar = receivableRepository.findById(receivableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + receivableId));

        BigDecimal fee = (cardFeePercent != null) ? cardFeePercent : BigDecimal.ZERO;
        BigDecimal feeForThisPayment;
        BigDecimal netAmount;

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            if (fee.compareTo(new BigDecimal("100")) >= 0) {
                throw new IllegalArgumentException("Taxa de cartão inválida: deve ser menor que 100%.");
            }
            feeForThisPayment = amountReceived
                    .multiply(fee)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            netAmount = amountReceived.subtract(feeForThisPayment);
        } else {
            feeForThisPayment = BigDecimal.ZERO;
            netAmount = amountReceived;
        }

        BigDecimal current = ar.getReceivedAmount() != null ? ar.getReceivedAmount() : BigDecimal.ZERO;
        BigDecimal newTotalReceived = current.add(netAmount);

        BigDecimal newTotalGross = (ar.getGrossReceivedAmount() != null ? ar.getGrossReceivedAmount() : BigDecimal.ZERO)
                .add(amountReceived);

        ar.setReceivedAmount(newTotalReceived);
        ar.setGrossReceivedAmount(newTotalGross);

        BigDecimal currentFeeAmount = ar.getFeeAmount() != null ? ar.getFeeAmount() : BigDecimal.ZERO;
        ar.setFeeAmount(currentFeeAmount.add(feeForThisPayment));

        if (paymentDate != null) ar.setPaymentDate(paymentDate);
        if (notes != null && !notes.isBlank()) ar.setNotes(notes);

        if (feeForThisPayment.compareTo(BigDecimal.ZERO) > 0) {
            String autoNote = String.format(
                    "[Taxa de %.2f%% = Custo de R$ %.2f | Bruto: R$ %.2f | Líquido no Caixa: R$ %.2f]",
                    fee, feeForThisPayment, amountReceived, netAmount);
            String existing = ar.getNotes();
            ar.setNotes((existing != null && !existing.isBlank() ? existing + "\n" : "") + autoNote);

            WorkOrder wo = ar.getWorkOrder();
            if (wo != null) {
                WorkOrderItem feeItem = new WorkOrderItem();
                feeItem.setWorkOrder(wo);
                String dateStr = paymentDate != null ? paymentDate.toString() : LocalDate.now().toString();

                feeItem.setDescription("[OP] Outros | " + dateStr + " | Taxa de Maquininha/Banco ||| Despesa Financeira Automática");
                feeItem.setQuantity(BigDecimal.valueOf(1));
                feeItem.setUnitCost(feeForThisPayment);
                feeItem.setUnitPrice(BigDecimal.ZERO);

                wo.getItems().add(feeItem);
                workOrderRepository.save(wo);
            }
        }

        // ATUALIZA O STATUS E INJETA A OBSERVAÇÃO DE PAGAMENTO PARCIAL E DATA FINAL AQUI NO SERVIÇO
        if (newTotalGross.compareTo(BigDecimal.ZERO) > 0
                && newTotalGross.compareTo(ar.getTotalAmount()) < 0) {
            ar.setStatus("partial");

            // Registra nas observações
            String dataStr = paymentDate != null ? paymentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String novaObs = "Pgto Parcial em " + dataStr + ": R$ " + amountReceived.toString();
            String obsAtual = ar.getNotes() != null ? ar.getNotes() : "";
            ar.setNotes(obsAtual.isEmpty() ? novaObs : obsAtual + "\n" + novaObs);

        } else if (newTotalGross.compareTo(ar.getTotalAmount()) >= 0) {
            ar.setStatus("received");
            if (ar.getPaymentDate() == null) {
                ar.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
            }
        }

        receivableRepository.save(ar);
    }

    @Transactional
    public void cancelReceivable(UUID receivableId) {
        AccountsReceivable ar = receivableRepository.findById(receivableId).orElseThrow();
        ar.setStatus("cancelled");
        receivableRepository.save(ar);
    }

    public java.util.Map<String, java.math.BigDecimal> getExpensesBySubcategory() {
        return payableRepository.findAll().stream()
                .filter(p -> p.getSubcategory() != null && !p.getSubcategory().trim().isEmpty())
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