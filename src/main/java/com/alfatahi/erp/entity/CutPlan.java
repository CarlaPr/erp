package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Plano de Corte. Uma Ordem de Serviço pode possuir 1..N planos de corte.
 * Totalmente integrado a WorkOrder (não é um módulo isolado).
 */
@Entity
@Table(name = "cut_plans")
public class CutPlan {

    public enum Status { draft, in_production, sent_to_supplier, done, cancelled }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    @JsonIgnoreProperties({"items", "quote", "hibernateLazyInitializer", "handler"})
    private WorkOrder workOrder;

    @Column(name = "plan_number", nullable = false)
    private Integer planNumber;

    private String title;

    @Enumerated(EnumType.STRING)
    private Status status = Status.draft;

    private String origin; // "quote" ou "manual" (herdado da OS)

    private String responsible;

    @ManyToOne
    @JoinColumn(name = "rule_set_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CutRuleSet ruleSet;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    private String notes;

    @OneToMany(mappedBy = "cutPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @JsonIgnoreProperties("cutPlan")
    private List<CutPlanItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "cutPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cutPlan")
    private List<CutPlanMaterial> materials = new ArrayList<>();

    @OneToMany(mappedBy = "cutPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt DESC")
    @JsonIgnoreProperties("cutPlan")
    @JsonIgnore
    private List<CutPlanHistory> history = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public Integer getPlanNumber() { return planNumber; }
    public void setPlanNumber(Integer planNumber) { this.planNumber = planNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getResponsible() { return responsible; }
    public void setResponsible(String responsible) { this.responsible = responsible; }
    public CutRuleSet getRuleSet() { return ruleSet; }
    public void setRuleSet(CutRuleSet ruleSet) { this.ruleSet = ruleSet; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<CutPlanItem> getItems() { return items; }
    public void setItems(List<CutPlanItem> items) { this.items = items; }
    public List<CutPlanMaterial> getMaterials() { return materials; }
    public void setMaterials(List<CutPlanMaterial> materials) { this.materials = materials; }
    public List<CutPlanHistory> getHistory() { return history; }
    public void setHistory(List<CutPlanHistory> history) { this.history = history; }

    // ── Painel de custos (calculado a partir dos snapshots de preço) ──

    public BigDecimal getGlassArea() {
        return items.stream()
                .map(CutPlanItem::getArea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getGlassCost() {
        return items.stream()
                .map(CutPlanItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getMaterialsCost(CutPlanMaterial.Category category) {
        return materials.stream()
                .filter(m -> m.getCategory() == category)
                .map(CutPlanMaterial::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getHardwareCost() { return getMaterialsCost(CutPlanMaterial.Category.HARDWARE); }
    public BigDecimal getAluminumCost() { return getMaterialsCost(CutPlanMaterial.Category.ALUMINUM); }
    public BigDecimal getSiliconeCost() { return getMaterialsCost(CutPlanMaterial.Category.SILICONE); }
    public BigDecimal getOtherMaterialsCost() { return getMaterialsCost(CutPlanMaterial.Category.OTHER); }

    public BigDecimal getTotalCost() {
        return getGlassCost().add(getHardwareCost()).add(getAluminumCost())
                .add(getSiliconeCost()).add(getOtherMaterialsCost());
    }

    public BigDecimal getEstimatedWeight() {
        return items.stream().map(CutPlanItem::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
