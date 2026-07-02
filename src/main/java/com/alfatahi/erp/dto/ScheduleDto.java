package com.alfatahi.erp.dto;

import com.alfatahi.erp.entity.ScheduleHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class ScheduleDto {

    private UUID id;

    private UUID quoteId;
    private String quoteNumber;

    private UUID workOrderId;
    private String workOrderNumber;

    private UUID clientId;
    private String clientName;

    private String serviceType;

    private LocalDateTime approvalDate;
    private LocalDate deadlineDate;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private Integer estimatedDurationMinutes;

    private String status;
    private String statusLabel;

    private String semaphore;

    private String clientAddress;
    private List<String> serviceDetails;

    private Long daysRemaining;

    private String responsible;
    private String team;
    private String observations;
    private String rescheduleReason;

    private List<ScheduleHistory> history;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public List<String> getServiceDetails() {
        return serviceDetails;
    }

    public void setServiceDetails(List<String> serviceDetails) {
        this.serviceDetails = serviceDetails;
    }

    public UUID getQuoteId() { return quoteId; }
    public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }

    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public UUID getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(UUID workOrderId) { this.workOrderId = workOrderId; }

    public String getWorkOrderNumber() { return workOrderNumber; }
    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }

    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getSemaphore() { return semaphore; }
    public void setSemaphore(String semaphore) { this.semaphore = semaphore; }

    public Long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(Long daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getRescheduleReason() { return rescheduleReason; }
    public void setRescheduleReason(String rescheduleReason) { this.rescheduleReason = rescheduleReason; }

    public List<ScheduleHistory> getHistory() { return history; }
    public void setHistory(List<ScheduleHistory> history) { this.history = history; }
}
