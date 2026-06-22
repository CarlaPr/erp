package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    public List<WorkOrder> listAll() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public WorkOrder save(WorkOrder workOrder) {
        // Gerador automático de número de O.S. sequencial básico caso venha vazio
        if (workOrder.getNumber() == null || workOrder.getNumber().isEmpty()) {
            workOrder.setNumber("OS-" + (workOrderRepository.count() + 1001));
        }

        // Cálculo automático da área baseado na largura e altura enviadas
        if (workOrder.getWidth() != null && workOrder.getHeight() != null) {
            workOrder.setArea(workOrder.getWidth().multiply(workOrder.getHeight()));
        }

        return workOrderRepository.save(workOrder);
    }

    public WorkOrder findById(UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("O.S. não encontrada: " + id));
    }

    public void delete(UUID id) {
        workOrderRepository.deleteById(id);
    }
}