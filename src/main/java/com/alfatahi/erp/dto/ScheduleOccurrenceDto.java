package com.alfatahi.erp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ScheduleOccurrenceDto {

    private UUID id;
    private LocalDate occurrenceDate;
    private LocalTime occurrenceTime;
    private Integer estimatedDurationMinutes;
    private String status;
    private String statusLabel;
    private String responsible;
    private String team;
    private String observations;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }

    public LocalTime getOccurrenceTime() { return occurrenceTime; }
    public void setOccurrenceTime(LocalTime occurrenceTime) { this.occurrenceTime = occurrenceTime; }

    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() { return statusLabel; }
    public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
