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
    private final BankAccountService bankAccountService;
    private final ExpenseAllocationRepository expenseAllocationRepository;
    private final WorkOrderItemRepository workOrderItemRepository;

    public FinanceService(AccountsPayableRepository payableRepository,
                          AccountsReceivableRepository receivableRepository,
                          WorkOrderRepository workOrderRepository,
                          FinancialMovementRepository financialMovementRepository,
                          BankAccountService bankAccountService,
                          ExpenseAllocationRepository expenseAllocationRepository,
                          WorkOrderItemRepository workOrderItemRepository) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.workOrderRepository = workOrderRepository;
        this.financialMovementRepository = financialMovementRepository;
        this.bankAccountService = bankAccountService;
        this.expenseAllocationRepository = expenseAllocationRepository;
        this.workOrderItemRepository = workOrderItemRepository;
    }

    public List<AccountsPayable> listAllPayables()       { return payableRepository.findAllByOrderByDueDateAsc(); }
    public List<AccountsReceivable> listAllReceivables() { return receivableRepository.findAllByOrderByDueDateAsc(); }
    public AccountsPayable savePayable(AccountsPayable p)     { return payableRepository.save(p); }
    public AccountsReceivable saveReceivable(AccountsReceivable r) { return receivableRepository.save(r); }

    public BigDecimal getTotalReceivables() { return receivableRepository.sumTotalReceivables(); }
    public BigDecimal getTotalPayables()    { return payableRepository.sumTotalPayables(); }

    @Transactional
    public void processPayablePayment(UUID payableId, BigDecimal amountPaid,
                                      LocalDate paymentDate, String paymentMethod, UUID bankAccountId, String notes) {
        AccountsPayable ap = payableRepository.findForUpdateById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + payableId));

        if (amountPaid == null) {
            amountPaid = ap.getBalance();
        }
        validatePayment(ap.getStatus(), amountPaid);
        LocalDate effectivePaymentDate = paymentDate != null ? paymentDate : FinancialPeriod.today();
        String effectivePaymentMethod = paymentMethod != null && !paymentMethod.isBlank()
                ? paymentMethod : ap.getPaymentMethod();
        FinancialMovement movement = createMovement(FinancialMovement.MovementType.SAIDA,
                amountPaid, effectivePaymentDate, effectivePaymentMethod, bankAccountId);
        movement.setAccountsPayable(ap);

        BigDecimal newTotalPaid = ap.getPaidAmount().add(amountPaid);
        ap.setPaidAmount(newTotalPaid);
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
        financialMovementRepository.save(movement);
    }

    @Transactional
    public void cancelPayable(UUID payableId) {
        AccountsPayable ap = payableRepository.findForUpdateById(payableId).orElseThrow();
        ap.setStatus("cancelled");
        payableRepository.save(ap);
        removeAllocationsAndCosts(payableId);
    }

    @Transactional
    public void processReceivablePayment(UUID receivableId, BigDecimal amountReceived,
                                         LocalDate paymentDate, BigDecimal cardFeePercent,
                                         BigDecimal discountAmount, String paymentMethod, UUID bankAccountId, String notes) {

        AccountsReceivable ar = receivableRepository.findForUpdateById(receivableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada: " + receivableId));

        validatePayment(ar.getStatus(), amountReceived);
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

        LocalDate effectivePaymentDate = paymentDate != null ? paymentDate : FinancialPeriod.today();
        String effectivePaymentMethod = paymentMethod != null && !paymentMethod.isBlank()
                ? paymentMethod : ar.getPaymentMethod();
        FinancialMovement movement = createMovement(FinancialMovement.MovementType.ENTRADA,
                netAmount, effectivePaymentDate, effectivePaymentMethod, bankAccountId);
        movement.setAccountsReceivable(ar);

        BigDecimal newNetTotal   = ar.getReceivedAmount().add(netAmount);
        BigDecimal newGrossTotal = ar.getGrossReceivedAmount().add(amountReceived);
        BigDecimal newFeeTotal   = ar.getFeeAmount().add(feeForThisPayment);

        ar.setReceivedAmount(newNetTotal);
        ar.setGrossReceivedAmount(newGrossTotal);
        ar.setFeeAmount(newFeeTotal);

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
        financialMovementRepository.save(movement);
    }

    @Transactional
    public void cancelReceivable(UUID receivableId) {
        AccountsReceivable ar = receivableRepository.findForUpdateById(receivableId).orElseThrow();
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

    private void validatePayment(String status, BigDecimal amount) {
        if (!"pending".equals(status) && !"partial".equals(status)) {
            throw new IllegalArgumentException("A conta precisa estar em aberto para registrar uma baixa.");
        }
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Informe um valor maior que zero com até duas casas decimais.");
        }
    }

    private FinancialMovement createMovement(FinancialMovement.MovementType type, BigDecimal amount,
                                              LocalDate date, String paymentMethod, UUID bankAccountId) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new IllegalArgumentException("Informe a forma de pagamento.");
        }
        FinancialMovement movement = new FinancialMovement();
        movement.setMovementDate(date);
        movement.setType(type);
        movement.setBalanceLocation(resolveBalanceLocation(paymentMethod));
        movement.setAmount(amount);
        movement.setPaymentMethod(paymentMethod);
        if (movement.getBalanceLocation() == FinancialMovement.BalanceLocation.BANK) {
            movement.setBankAccount(bankAccountService.requireAccount(bankAccountId));
        }
        return movement;
    }

    private FinancialMovement.BalanceLocation resolveBalanceLocation(String paymentMethod) {
        String normalized = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        return List.of("CASH", "DINHEIRO", "MONEY").contains(normalized)
                ? FinancialMovement.BalanceLocation.CASH
                : FinancialMovement.BalanceLocation.BANK;
    }

    public record AllocationInput(UUID workOrderId, BigDecimal value, String description) {}

    @Transactional
    public void replaceAllocations(UUID payableId, List<AllocationInput> inputs) {
        AccountsPayable ap = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

        List<ExpenseAllocation> existing = expenseAllocationRepository.findByAccountsPayableId(payableId);

        List<AllocationInput> validInputs = (inputs == null ? List.<AllocationInput>of() : inputs).stream()
                .filter(i -> i.workOrderId() != null && i.value() != null && i.value().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        java.util.Map<UUID, ExpenseAllocation> existingByWorkOrder = new java.util.LinkedHashMap<>();
        for (ExpenseAllocation ea : existing) {
            if (ea.getWorkOrder() != null) existingByWorkOrder.put(ea.getWorkOrder().getId(), ea);
        }

        java.util.Set<UUID> keptWorkOrderIds = new java.util.HashSet<>();
        for (AllocationInput input : validInputs) {
            keptWorkOrderIds.add(input.workOrderId());
        }

         for (ExpenseAllocation ea : existing) {
            UUID woId = ea.getWorkOrder() != null ? ea.getWorkOrder().getId() : null;
            if (woId == null || !keptWorkOrderIds.contains(woId)) {
                deleteAllocationAndCost(ea);
            }
        }

        for (AllocationInput input : validInputs) {
            ExpenseAllocation ea = existingByWorkOrder.get(input.workOrderId());
            String description = (input.description() != null && !input.description().isBlank())
                    ? input.description() : ap.getDescription();

            if (ea != null) {
               ea.setValue(input.value());
                ea.setDescription(description);
                ea.setPercentage(computePercentage(input.value(), ap.getTotalAmount()));
                syncWorkOrderItem(ea, description, input.value());
                expenseAllocationRepository.save(ea);
            } else {
                WorkOrder wo = workOrderRepository.findById(input.workOrderId())
                        .orElseThrow(() -> new RuntimeException("OS não encontrada"));

                ExpenseAllocation newAllocation = new ExpenseAllocation();
                newAllocation.setAccountsPayable(ap);
                newAllocation.setWorkOrder(wo);
                newAllocation.setValue(input.value());
                newAllocation.setDescription(description);
                newAllocation.setPercentage(computePercentage(input.value(), ap.getTotalAmount()));
                newAllocation = expenseAllocationRepository.save(newAllocation);

                WorkOrderItem item = new WorkOrderItem();
                item.setWorkOrder(wo);
                item.setDescription(description);
                item.setQuantity(BigDecimal.ONE);
                item.setUnitCost(input.value());
                item.setUnitPrice(BigDecimal.ZERO);
                item.setSourceExpenseAllocationId(newAllocation.getId());
                item = workOrderItemRepository.save(item);

                newAllocation.setWorkOrderItem(item);
                expenseAllocationRepository.save(newAllocation);
            }
        }
    }

    @Transactional
    public void removeAllocationsAndCosts(UUID payableId) {
        List<ExpenseAllocation> existing = expenseAllocationRepository.findByAccountsPayableId(payableId);
        for (ExpenseAllocation ea : existing) {
            deleteAllocationAndCost(ea);
        }
    }

    private void deleteAllocationAndCost(ExpenseAllocation ea) {
        WorkOrderItem item = ea.getWorkOrderItem();
        expenseAllocationRepository.delete(ea);
        if (item != null && item.getId() != null) {
            workOrderItemRepository.deleteById(item.getId());
        }
    }

    private void syncWorkOrderItem(ExpenseAllocation ea, String description, BigDecimal value) {
        WorkOrderItem item = ea.getWorkOrderItem();
        if (item == null) {
            item = new WorkOrderItem();
            item.setWorkOrder(ea.getWorkOrder());
            item.setQuantity(BigDecimal.ONE);
            item.setUnitPrice(BigDecimal.ZERO);
            item.setSourceExpenseAllocationId(ea.getId());
        }
        item.setDescription(description);
        item.setUnitCost(value);
        workOrderItemRepository.save(item);
        ea.setWorkOrderItem(item);
    }

    private BigDecimal computePercentage(BigDecimal value, BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0 || value == null) {
            return BigDecimal.ZERO;
        }
        return value.multiply(new BigDecimal("100"))
                .divide(totalAmount, 2, RoundingMode.HALF_UP);
    }
}
