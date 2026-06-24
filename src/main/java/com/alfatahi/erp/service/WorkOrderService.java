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

    public WorkOrderService(WorkOrderRepository workOrderRepository, WorkOrderItemRepository itemRepository) {
        this.workOrderRepository = workOrderRepository;
        this.itemRepository = itemRepository;
    }


    public List<WorkOrder> listAll() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public WorkOrder save(WorkOrder workOrder) {
        // 1. Gera o número da OS apenas se for nova (ID nulo)
        if (workOrder.getId() == null) {
            if (workOrder.getNumber() == null || workOrder.getNumber().isEmpty()) {
                long count = workOrderRepository.count();
                workOrder.setNumber("OS-" + (1001 + count));
            }
        }

        // 2. Cálculo da Área (Seguro contra nulos)
        if (workOrder.getWidth() != null && workOrder.getHeight() != null) {
            workOrder.setArea(workOrder.getWidth().multiply(workOrder.getHeight()));
        }

        // 3. Garante a bidirecionalidade dos itens antes de salvar
        // Isto evita que o Hibernate perca a referência de qual OS pertence ao item
        if (workOrder.getItems() != null) {
            for (WorkOrderItem item : workOrder.getItems()) {
                item.setWorkOrder(workOrder);
            }
        }

        // 4. Save and Flush para garantir que o Hibernate valida a integridade AGORA
        return workOrderRepository.saveAndFlush(workOrder);
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