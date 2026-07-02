package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.repository.WorkOrderItemRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository itemRepository;
    private final ScheduleService scheduleService;

    public WorkOrderService(WorkOrderRepository workOrderRepository, WorkOrderItemRepository itemRepository, ScheduleService scheduleService) {
        this.workOrderRepository = workOrderRepository;
        this.itemRepository = itemRepository;
        this.scheduleService = scheduleService;
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
}