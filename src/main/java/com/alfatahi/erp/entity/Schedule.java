package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "commercial_schedules")
public class Schedule {

    public static final String STATUS_AGUARDANDO_AGENDAMENTO = "AGUARDANDO_AGENDAMENTO";
    public static final String STATUS_AGENDADO = "AGENDADO";
    public static final String STATUS_CONFIRMADO = "CONFIRMADO";
    public static final String STATUS_EM_EXECUCAO = "EM_EXECUCAO";
    public static final String STATUS_CONCLUIDO = "CONCLUIDO";
    public static final String STATUS_ATRASADO = "ATRASADO";
    public static final String STATUS_REAGENDADO = "REAGENDADO";
    public static final String STATUS_CANCELADO = "CANCELADO";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "quote_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"items", "workOrder", "client"})
    private Quote quote;

    @OneToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    @JsonIgnoreProperties({"items", "quote", "client"})
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnoreProperties({"quotes", "workOrders"})
    private Client client;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(nullable = false, length = 30)
    private String status = STATUS_AGUARDANDO_AGENDAMENTO;

    private String responsible;

    private String team;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(name = "reschedule_reason", columnDefinition = "text")
    private String rescheduleReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

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

    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }

    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getRescheduleReason() { return rescheduleReason; }
    public void setRescheduleReason(String rescheduleReason) { this.rescheduleReason = rescheduleReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
