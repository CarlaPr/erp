package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/** Registro de auditoria: quem criou/alterou o Plano de Corte, quando e por quê. */
@Entity
@Table(name = "cut_plan_history")
public class CutPlanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "cut_plan_id", nullable = false)
    private CutPlan cutPlan;

    @Column(nullable = false, length = 50)
    private String action; // created, item_added, item_updated, item_removed, optimized, pdf_generated, status_changed...

    private String details;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutPlan getCutPlan() { return cutPlan; }
    public void setCutPlan(CutPlan cutPlan) { this.cutPlan = cutPlan; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
