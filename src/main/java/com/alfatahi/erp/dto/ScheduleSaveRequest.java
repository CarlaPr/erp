package com.alfatahi.erp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Corpo esperado por POST /agenda/save-ajax.
 * Usado tanto para o primeiro agendamento quanto para edição/reagendamento
 * (a UI é a mesma modal em ambos os casos).
 */
public class ScheduleSaveRequest {

    private UUID id;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String status;
    private String responsible;
    private String team;
    private Integer estimatedDurationMinutes;
    private String observations;
    private String reason;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public LocalTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
