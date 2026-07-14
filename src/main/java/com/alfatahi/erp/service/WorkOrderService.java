package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.WorkOrderItemRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository itemRepository;
    private final ScheduleService scheduleService;
    private final AccountsReceivableRepository receivableRepository;
    private final PaymentTermsService paymentTermsService;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderItemRepository itemRepository,
                            ScheduleService scheduleService,
                            AccountsReceivableRepository receivableRepository,
                            PaymentTermsService paymentTermsService) {
        this.workOrderRepository = workOrderRepository;
        this.itemRepository = itemRepository;
        this.scheduleService = scheduleService;
        this.receivableRepository = receivableRepository;
        this.paymentTermsService = paymentTermsService;
    }

    public List<WorkOrder> listAll() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public WorkOrder save(WorkOrder workOrder) {
        if (workOrder.getId() == null) {
            if (workOrder.getNumber() == null || workOrder.getNumber().isBlank()) {
                Integer ultimoNumero = workOrderRepository.findMaxWorkOrderSequence();
                workOrder.setNumber("OS-" + (ultimoNumero + 1));
            }
        }

        if (workOrder.getWidth() != null && workOrder.getHeight() != null) {
            workOrder.setArea(workOrder.getWidth().multiply(workOrder.getHeight()));
        }

        if (workOrder.getItems() != null) {
            for (WorkOrderItem item : workOrder.getItems()) {
                item.setWorkOrder(workOrder);
            }
        }

        if (workOrder.getTotalValue() == null || workOrder.getTotalValue().compareTo(BigDecimal.ZERO) == 0) {
            workOrder.setTotalValue(calculateTotalValueFromItems(workOrder));
        }

        WorkOrder savedWorkOrder = workOrderRepository.saveAndFlush(workOrder);

        if (savedWorkOrder.getInstallDate() != null) {
            scheduleService.syncDeadlineFromWorkOrder(savedWorkOrder.getId(), savedWorkOrder.getInstallDate());
        }

        return savedWorkOrder;
    }

    public WorkOrder findById(UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada: " + id));
    }

    public void delete(UUID id) {
        workOrderRepository.deleteById(id);
    }

    public BigDecimal calculateObraCost(UUID workOrderId) {
        return itemRepository.findByWorkOrderId(workOrderId).stream()
                .map(WorkOrderItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalValueFromItems(WorkOrder workOrder) {
        if (workOrder.getItems() == null || workOrder.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return workOrder.getItems().stream()
                .map(item -> item.getQuantity().multiply(item.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void createReceivablesForWorkOrder(UUID workOrderId,
                                              String paymentMethod,
                                              String paymentPlan,
                                              LocalDate firstDueDate) {
        WorkOrder workOrder = findById(workOrderId);

        if (workOrder.getTotalValue() == null || workOrder.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ordem de Serviço deve ter valor total maior que zero");
        }

        LocalDate deliveryDate = firstDueDate != null ? firstDueDate
                : (workOrder.getInstallDate() != null ? workOrder.getInstallDate() : LocalDate.now().plusDays(15));

        List<PaymentTermsService.PlannedInstallment> plan =
                paymentTermsService.generateInstallments(
                        paymentMethod, paymentPlan,
                        workOrder.getTotalValue(), LocalDate.now(), deliveryDate);

        int total = plan.size();
        for (int i = 0; i < total; i++) {
            PaymentTermsService.PlannedInstallment inst = plan.get(i);
            AccountsReceivable parcela = new AccountsReceivable();
            parcela.setClient(workOrder.getClient());
            parcela.setWorkOrder(workOrder);
            parcela.setPaymentMethod(paymentMethod);
            parcela.setPaymentStage(inst.getStage());
            parcela.setReferenceMonth(inst.getDueDate().withDayOfMonth(1));

            String desc;
            if (total == 1) {
                desc = "Ref. " + workOrder.getNumber();
            } else {
                String stageLabel = PaymentTermsService.STAGE_ENTRADA.equals(inst.getStage())
                        ? "Entrada (50%)" : "Saldo na Entrega (50%)";
                desc = "Ref. " + workOrder.getNumber() + " — " + stageLabel;
            }
            parcela.setDescription(desc);
            parcela.setTotalAmount(inst.getAmount());
            parcela.setInstallments(total);
            parcela.setDueDate(inst.getDueDate());
            parcela.setStatus("pending");
            receivableRepository.save(parcela);
        }
    }

    @Transactional
    public void createReceivablesForWorkOrder(UUID workOrderId, int installments,
                                              String paymentMethod, LocalDate firstDueDate) {
        createReceivablesForWorkOrder(workOrderId, paymentMethod,
                PaymentTermsService.PLAN_SPLIT_50_50, firstDueDate);
    }
}
