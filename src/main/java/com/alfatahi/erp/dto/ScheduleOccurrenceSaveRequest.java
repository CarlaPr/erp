package com.alfatahi.erp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;


public class ScheduleOccurrenceSaveRequest {

    private UUID scheduleId;
    private UUID occurrenceId;

    private LocalDate occurrenceDate;
    private LocalTime occurrenceTime;
    private Integer estimatedDurationMinutes;
    private String status;
    private String responsible;
    private String team;
    private String observations;
    private String reason;

    public UUID getScheduleId() { return scheduleId; }
    public void setScheduleId(UUID scheduleId) { this.scheduleId = scheduleId; }

    public UUID getOccurrenceId() { return occurrenceId; }
    public void setOccurrenceId(UUID occurrenceId) { this.occurrenceId = occurrenceId; }

    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }

    public LocalTime getOccurrenceTime() { return occurrenceTime; }
    public void setOccurrenceTime(LocalTime occurrenceTime) { this.occurrenceTime = occurrenceTime; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
