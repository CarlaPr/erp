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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

        // A agenda é a origem das datas; salvar a O.S. não altera o prazo nem o agendamento.
        scheduleService.applyAgendaDates(workOrder);

        // O número é único em banco (ver migração V31); em caso de colisão rara (duas O.S.
        // novas salvas quase ao mesmo tempo), a gravação falha com DataIntegrityViolationException
        // em vez de silenciosamente duplicar o número — ver tratamento em WorkOrderController.
        WorkOrder saved = workOrderRepository.saveAndFlush(workOrder);
        if ("cancelled".equals(saved.getStatus()) || "canceled".equals(saved.getStatus())) {
            scheduleService.onWorkOrderCancelled(saved.getId());
        }
        return saved;
    }

    public WorkOrder findById(UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        WorkOrder wo = workOrderRepository.findById(id).orElse(null);

        if (wo != null) {
            validateDeletion(wo);
            scheduleService.onWorkOrderDeleted(id);

            if (wo.getItems() != null && !wo.getItems().isEmpty()) {
                itemRepository.deleteAll(wo.getItems());
            }

            workOrderRepository.delete(wo);
        }
    }

    /**
     * A exclusão de uma O.S. é sempre permitida, mesmo quando vinculada a um orçamento
     * aprovado: o vínculo é desfeito junto com a exclusão (ver WorkOrderController#delete)
     * e, como o orçamento continua com status "approved" sem nenhuma O.S. associada, ele
     * volta a aparecer em "Orçamentos aprovados sem O.S." para que uma nova O.S. seja
     * gerada — sem risco de duplicidade, já que quote_id é único em work_orders.
     */
    public void validateDeletion(WorkOrder workOrder) {
        // Sem restrições adicionais no momento; método mantido como ponto único de
        // extensão para futuras regras de validação antes da exclusão.
    }

    /**
     * Quando o valor total da O.S. muda (edição direta da O.S. ou sincronização a partir
     * de um orçamento aprovado editado), redistribui proporcionalmente o valor entre as
     * contas a receber ainda ativas (não canceladas) vinculadas a ela, preservando o
     * número de parcelas/estágios já lançados.
     */
    @Transactional
    public void redistributeReceivablesIfTotalChanged(WorkOrder workOrder, BigDecimal oldTotal, String changeReasonMsg) {
        if (workOrder == null || workOrder.getId() == null) return;
        if (oldTotal == null || workOrder.getTotalValue().compareTo(oldTotal) == 0) return;

        List<AccountsReceivable> receivables = receivableRepository.findAll().stream()
                .filter(r -> r.getWorkOrder() != null && r.getWorkOrder().getId().equals(workOrder.getId()))
                .filter(r -> !"cancelled".equals(r.getStatus()))
                .collect(Collectors.toList());

        if (receivables.isEmpty()) return;

        int installmentsCount = receivables.size();
        BigDecimal newTotal = workOrder.getTotalValue();

        BigDecimal installmentValue = newTotal.divide(new BigDecimal(installmentsCount), 2, RoundingMode.HALF_UP);
        BigDecimal sumPrevious = installmentValue.multiply(new BigDecimal(installmentsCount - 1));
        BigDecimal lastInstallment = newTotal.subtract(sumPrevious);

        String reason = (changeReasonMsg != null && !changeReasonMsg.isBlank())
                ? changeReasonMsg : "Valor ajustado conforme alteração na O.S.";

        for (int i = 0; i < installmentsCount; i++) {
            AccountsReceivable ar = receivables.get(i);
            ar.setTotalAmount(i == installmentsCount - 1 ? lastInstallment : installmentValue);
            String currentNotes = ar.getNotes() != null ? ar.getNotes() : "";
            ar.setNotes(currentNotes + "\n[Sistema] " + reason);
            receivableRepository.save(ar);
        }
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


}