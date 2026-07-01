package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Timeline / histórico de um registro da Agenda Comercial.
 * Ex.: Criado -> Agendado -> Reagendado -> Concluído.
 */
@Entity
@Table(name = "commercial_schedule_history")
public class ScheduleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    @JsonIgnore
    private Schedule schedule;

    @Column(name = "event_date")
    private LocalDateTime eventDate = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String action;

    private String username;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(columnDefinition = "text")
    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Schedule getSchedule() { return schedule; }
    public void setSchedule(Schedule schedule) { this.schedule = schedule; }

    public LocalDateTime getEventDate() { return eventDate; }
    public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
