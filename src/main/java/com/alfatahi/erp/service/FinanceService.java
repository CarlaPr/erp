package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FinanceService {

    private final AccountsPayableRepository payableRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final WorkOrderRepository workOrderRepository;
    private final FinancialMovementRepository financialMovementRepository;

    public FinanceService(AccountsPayableRepository payableRepository,
                          AccountsReceivableRepository receivableRepository,
                          WorkOrderRepository workOrderRepository,
                          FinancialMovementRepository financialMovementRepository) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.workOrderRepository = workOrderRepository;
        this.financialMovementRepository = financialMovementRepository;
    }

    public List<AccountsPayable> listAllPayables()       { return payableRepository.findAllByOrderByDueDateAsc(); }
    public List<AccountsReceivable> listAllReceivables() { return receivableRepository.findAllByOrderByDueDateAsc(); }
    public AccountsPayable savePayable(AccountsPayable p)     { return payableRepository.save(p); }
    public AccountsReceivable saveReceivable(AccountsReceivable r) { return receivableRepository.save(r); }

    public BigDecimal getTotalReceivables() { return receivableRepository.sumTotalReceivables(); }
    public BigDecimal getTotalPayables()    { return payableRepository.sumTotalPayables(); }

    @Transactional
    public void processPayablePayment(UUID payableId, BigDecimal amountPaid,
                                      LocalDate paymentDate, String paymentMethod, String notes) {
        AccountsPayable ap = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + payableId));

        BigDecimal newTotalPaid = ap.getPaidAmount().add(amountPaid);
        ap.setPaidAmount(newTotalPaid);

        LocalDate effectivePaymentDate = paymentDate != null ? paymentDate : LocalDate.now();
        String effectivePaymentMethod = paymentMethod != null && !paymentMethod.isBlank()
                ? paymentMethod : ap.getPaymentMethod();

        ap.setPaymentDate(effectivePaymentDate);
        if (effectivePaymentMethod != null && !effectivePaymentMethod.isBlank())
            ap.setPaymentMethod(effectivePaymentMethod);
        if (notes != null && !notes.isBlank()) ap.setNotes(notes);

        if (newTotalPaid.compareTo(BigDecimal.ZERO) > 0
                && newTotalPaid.compareTo(ap.getTotalAmount()) < 0) {
            ap.setStatus("partial");
        } else if (newTotalPaid.compareTo(ap.getTotalAmount()) >= 0) {
            ap.setStatus("paid");
        }
        payableRepository.save(ap);
        recordPayableMovement(ap, amountPaid, effectivePaymentDate, effectivePaymentMethod);
    }

    @Transactional
    public void cancelPayable(UUID payableId) {
        AccountsPayable ap = payableRepository.findById(payableId).orElseThrow();
        ap.setStatus("cancelled");
        payableRepository.save(ap);
    }

    @Transactional
    public void processReceivablePayment(UUID receivableId, BigDecimal amountReceived,
                                         LocalDate paymentDate, BigDecimal cardFeePercent,
                                         BigDecimal discountAmount, String paymentMethod, String notes) {

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

        BigDecimal newNetTotal   = ar.getReceivedAmount().add(netAmount);
        BigDecimal newGrossTotal = ar.getGrossReceivedAmount().add(amountReceived);
        BigDecimal newFeeTotal   = ar.getFeeAmount().add(feeForThisPayment);

        ar.setReceivedAmount(newNetTotal);
        ar.setGrossReceivedAmount(newGrossTotal);
        ar.setFeeAmount(newFeeTotal);

        LocalDate effectivePaymentDate = paymentDate != null ? paymentDate : LocalDate.now();
        String effectivePaymentMethod = paymentMethod != null && !paymentMethod.isBlank()
                ? paymentMethod : ar.getPaymentMethod();

        ar.setPaymentDate(effectivePaymentDate);
        if (effectivePaymentMethod != null && !effectivePaymentMethod.isBlank())
            ar.setPaymentMethod(effectivePaymentMethod);
        if (cardFeePercent != null && cardFeePercent.compareTo(BigDecimal.ZERO) > 0)
            ar.setCardFeePercentage(cardFeePercent);

        if (discountAmount != null) {
            ar.setDiscount(discountAmount);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dateStr = effectivePaymentDate.format(fmt);
        String autoNote;
        if (feeForThisPayment.compareTo(BigDecimal.ZERO) > 0) {
            autoNote = String.format("[%s] Recebido R$ %.2f bruto | Taxa %.2f%% = R$ %.2f | Líquido recebido: R$ %.2f",
                    dateStr, amountReceived, fee, feeForThisPayment, netAmount);
        } else {
            autoNote = String.format("[%s] Recebido R$ %.2f", dateStr, amountReceived);
        }

        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            autoNote += String.format(" | Desconto Concedido: R$ %.2f", discountAmount);
        }

        if (notes != null && !notes.isBlank()) autoNote = autoNote + " | Obs: " + notes;
        String existingNotes = ar.getNotes();
        ar.setNotes((existingNotes != null && !existingNotes.isBlank() ? existingNotes + "\n" : "") + autoNote);

        if (ar.getWorkOrder() != null) {
            WorkOrder wo = ar.getWorkOrder();
            boolean hasOsChanges = false;
            String opDateStr = effectivePaymentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            if (wo.getItems() == null) {
                wo.setItems(new ArrayList<>());
            }

            if (feeForThisPayment.compareTo(BigDecimal.ZERO) > 0) {
                WorkOrderItem opFee = new WorkOrderItem();
                opFee.setWorkOrder(wo);

                opFee.setDescription("[OP] Outros | " + opDateStr + " | Taxa " + fee + "% ||| Taxa de Maquininha — " + (paymentMethod != null ? paymentMethod : "Automática"));
                opFee.setQuantity(BigDecimal.ONE);
                opFee.setUnitCost(feeForThisPayment);
                opFee.setUnitPrice(BigDecimal.ZERO);
                wo.getItems().add(opFee);
                hasOsChanges = true;
            }

            if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                WorkOrderItem opDiscount = new WorkOrderItem();
                opDiscount.setWorkOrder(wo);
                opDiscount.setDescription("[OP] Outros | " + opDateStr + " | ||| Desconto Concedido — " + (paymentMethod != null ? paymentMethod : "Automático"));
                opDiscount.setQuantity(BigDecimal.ONE);
                opDiscount.setUnitCost(discountAmount);
                opDiscount.setUnitPrice(BigDecimal.ZERO);
                wo.getItems().add(opDiscount);
                hasOsChanges = true;
            }

            if (hasOsChanges) {
                workOrderRepository.save(wo);
            }
        }

        if (newGrossTotal.compareTo(BigDecimal.ZERO) > 0
                && newGrossTotal.compareTo(ar.getTotalAmount()) < 0) {
            ar.setStatus("partial");
        } else if (newGrossTotal.compareTo(ar.getTotalAmount()) >= 0) {
            ar.setStatus("received");
            if (ar.getPaymentDate() == null) {
                ar.setPaymentDate(effectivePaymentDate);
            }
        }

        receivableRepository.save(ar);
        recordReceivableMovement(ar, netAmount, effectivePaymentDate, effectivePaymentMethod);
    }

    @Transactional
    public void cancelReceivable(UUID receivableId) {
        AccountsReceivable ar = receivableRepository.findById(receivableId).orElseThrow();
        ar.setStatus("cancelled");
        receivableRepository.save(ar);
    }

    public java.util.Map<String, BigDecimal> getExpensesBySubcategory() {
        return payableRepository.findAll().stream()
                .filter(p -> p.getSubcategory() != null && !p.getSubcategory().trim().isEmpty())
                .collect(java.util.stream.Collectors.groupingBy(
                        AccountsPayable::getSubcategory,
                        java.util.stream.Collectors.reducing(
                                BigDecimal.ZERO, AccountsPayable::getTotalAmount, BigDecimal::add)));
    }

    private void recordPayableMovement(AccountsPayable payable, BigDecimal amount,
                                       LocalDate date, String paymentMethod) {
        FinancialMovement movement = new FinancialMovement();
        movement.setMovementDate(date);
        movement.setType(FinancialMovement.MovementType.SAIDA);
        movement.setBalanceLocation(resolveBalanceLocation(paymentMethod));
        movement.setAmount(amount);
        movement.setPaymentMethod(paymentMethod);
        movement.setAccountsPayable(payable);
        financialMovementRepository.save(movement);
    }

    private void recordReceivableMovement(AccountsReceivable receivable, BigDecimal amount,
                                          LocalDate date, String paymentMethod) {
        FinancialMovement movement = new FinancialMovement();
        movement.setMovementDate(date);
        movement.setType(FinancialMovement.MovementType.ENTRADA);
        movement.setBalanceLocation(resolveBalanceLocation(paymentMethod));
        movement.setAmount(amount);
        movement.setPaymentMethod(paymentMethod);
        movement.setAccountsReceivable(receivable);
        financialMovementRepository.save(movement);
    }

    private FinancialMovement.BalanceLocation resolveBalanceLocation(String paymentMethod) {
        String normalized = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        return List.of("CASH", "DINHEIRO", "MONEY").contains(normalized)
                ? FinancialMovement.BalanceLocation.CASH
                : FinancialMovement.BalanceLocation.BANK;
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