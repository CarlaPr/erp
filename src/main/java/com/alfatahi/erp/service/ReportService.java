package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.WorkOrderItemReportDto;
import com.alfatahi.erp.dto.WorkOrderReportDto;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centraliza a montagem dos dados usados nos relatórios de Ordem de Serviço
 * (individual e consolidado). Outros relatórios (financeiro, contábil) usam
 * diretamente os repositórios/serviços já existentes (Contas a Pagar/Receber,
 * DRE, Fluxo de Caixa) através do ReportController.
 */
@Service
public class ReportService {

    private final WorkOrderRepository workOrderRepository;

    public ReportService(WorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional(readOnly = true)
    public WorkOrderReportDto getWorkOrderReport(UUID id) {
        WorkOrder wo = workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada: " + id));
        return toDto(wo);
    }

    /**
     * Filtra as Ordens de Serviço por período (data de criação), cliente e status.
     * Qualquer filtro pode vir nulo/vazio, o que significa "sem restrição" naquele campo.
     */
    @Transactional(readOnly = true)
    public List<WorkOrderReportDto> filterWorkOrders(LocalDate from, LocalDate to, UUID clientId, String status) {
        LocalDateTime start = from != null ? LocalDateTime.of(from, LocalTime.MIN) : null;
        LocalDateTime end = to != null ? LocalDateTime.of(to, LocalTime.MAX) : null;

        return workOrderRepository.findAllWithItemsOrderByCreatedAtDesc().stream()
                .filter(wo -> start == null || wo.getCreatedAt() == null || !wo.getCreatedAt().isBefore(start))
                .filter(wo -> end == null || wo.getCreatedAt() == null || !wo.getCreatedAt().isAfter(end))
                .filter(wo -> clientId == null || (wo.getClient() != null && clientId.equals(wo.getClient().getId())))
                .filter(wo -> status == null || status.isBlank() || status.equalsIgnoreCase(wo.getStatus()))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private WorkOrderReportDto toDto(WorkOrder wo) {
        WorkOrderReportDto dto = new WorkOrderReportDto();
        dto.setId(wo.getId());
        dto.setNumber(wo.getNumber());
        dto.setTitle(wo.getTitle());
        dto.setStatus(wo.getStatus());
        dto.setCategoryName(wo.getCategory() != null ? wo.getCategory().getName() : null);
        dto.setCreatedAt(wo.getCreatedAt());
        dto.setInstallDate(wo.getInstallDate());
        dto.setWidth(wo.getWidth());
        dto.setHeight(wo.getHeight());
        dto.setArea(wo.getArea());

        if (wo.getClient() != null) {
            dto.setClientName(wo.getClient().getName());
            dto.setClientDocument(wo.getClient().getDocument());
            dto.setClientPhone(wo.getClient().getPhone());
            dto.setClientCity(wo.getClient().getCity());
        } else {
            dto.setClientName("Consumidor Final");
        }

        List<WorkOrderItemReportDto> items = wo.getItems() == null ? List.of() :
                wo.getItems().stream()
                        .map(this::toItemDto)
                        .collect(Collectors.toList());
        dto.setItems(items);

        return dto;
    }

    private WorkOrderItemReportDto toItemDto(WorkOrderItem item) {
        return new WorkOrderItemReportDto(
                item.getDescription(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getUnitPrice()
        );
    }
}
