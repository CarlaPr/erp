package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.repository.WorkOrderItemRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository itemRepository;
    private final ScheduleService scheduleService;
    private final AccountsReceivableRepository receivableRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderItemRepository itemRepository,
                            ScheduleService scheduleService,
                            AccountsReceivableRepository receivableRepository) {
        this.workOrderRepository = workOrderRepository;
        this.itemRepository = itemRepository;
        this.scheduleService = scheduleService;
        this.receivableRepository = receivableRepository;
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
    public void createReceivablesForWorkOrder(UUID workOrderId, int installments, String paymentMethod, LocalDate firstDueDate) {
        WorkOrder workOrder = findById(workOrderId);

        if (workOrder.getTotalValue() == null || workOrder.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ordem de Serviço deve ter valor total maior que zero");
        }

        int numParcelas = installments > 0 ? installments : 1;

        BigDecimal valorParcela = workOrder.getTotalValue()
                .divide(new BigDecimal(numParcelas), 2, RoundingMode.HALF_UP);
        BigDecimal somaParcelasAnteriores = valorParcela.multiply(new BigDecimal(numParcelas - 1));
        BigDecimal valorUltimaParcela = workOrder.getTotalValue().subtract(somaParcelasAnteriores);

        for (int i = 0; i < numParcelas; i++) {
            boolean isUltima = (i == numParcelas - 1);
            AccountsReceivable parcela = new AccountsReceivable();

            parcela.setClient(workOrder.getClient());
            parcela.setWorkOrder(workOrder);
            parcela.setPaymentMethod(paymentMethod);
            parcela.setDescription(numParcelas == 1
                    ? "Ref. " + workOrder.getNumber()
                    : "Ref. " + workOrder.getNumber() + " — Parcela " + (i+1) + "/" + numParcelas);

            parcela.setTotalAmount(isUltima ? valorUltimaParcela : valorParcela);

            LocalDate dueDate = numParcelas == 1
                    ? (firstDueDate != null ? firstDueDate : workOrder.getInstallDate())
                    : (firstDueDate != null ? firstDueDate : workOrder.getInstallDate()).plusMonths(i + 1);

            parcela.setDueDate(dueDate);
            parcela.setStatus("pending");

            receivableRepository.save(parcela);
        }
    }
}