package com.alfatahi.erp.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;


public class TechnicalVisitSaveRequest {

    private UUID id;
    private UUID clientId;
    private LocalDate visitDate;
    private LocalTime visitTime;
    private String notes;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getClientId() { return clientId; }
    public void setClientId(UUID clientId) { this.clientId = clientId; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public LocalTime getVisitTime() { return visitTime; }
    public void setVisitTime(LocalTime visitTime) { this.visitTime = visitTime; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
