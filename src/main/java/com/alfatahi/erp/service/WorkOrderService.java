package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.repository.WorkOrderItemRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderItemRepository itemRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository, WorkOrderItemRepository itemRepository) {
        this.workOrderRepository = workOrderRepository;
        this.itemRepository = itemRepository;
    }

    public List<WorkOrder> listAll() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public WorkOrder save(WorkOrder workOrder) {
        if (workOrder.getNumber() == null || workOrder.getNumber().isEmpty()) {
            workOrder.setNumber("OS-" + (workOrderRepository.count() + 1001));
        }

        if (workOrder.getWidth() != null && workOrder.getHeight() != null) {
            workOrder.setArea(workOrder.getWidth().multiply(workOrder.getHeight()));
        }

        return workOrderRepository.save(workOrder);
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