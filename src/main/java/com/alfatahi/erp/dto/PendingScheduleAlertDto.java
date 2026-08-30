package com.alfatahi.erp.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Representa uma Ordem de Serviço com prazo de entrega próximo de vencer (ou já vencido)
 * e que ainda não possui nenhum agendamento definido (data marcada na Agenda Comercial).
 * Usado para alimentar a modal de atenção exibida para usuários da role VENDAS.
 */
public class PendingScheduleAlertDto {

    private UUID scheduleId;
    private UUID workOrderId;
    private String workOrderNumber;

    private UUID quoteId;
    private String quoteNumber;

    private UUID clientId;
    private String clientName;

    private String serviceType;
    private List<String> serviceDetails;

    private LocalDate deadlineDate;
    private Long daysRemaining;
    private boolean overdue;

    public UUID getScheduleId() { return scheduleId; }
    public void setScheduleId(UUID scheduleId) { this.scheduleId = scheduleId; }

    public UUID getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(UUID workOrderId) { this.workOrderId = workOrderId; }

    public String getWorkOrderNumber() { return workOrderNumber; }
    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

    public UUID getQuoteId() { return quoteId; }
    public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }

    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public List<String> getServiceDetails() { return serviceDetails; }
    public void setServiceDetails(List<String> serviceDetails) { this.serviceDetails = serviceDetails; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public Long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }

    public boolean isOverdue() { return overdue; }
    public void setOverdue(boolean overdue) { this.overdue = overdue; }
}
