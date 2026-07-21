package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/** Chanfro (corte de canto a 45°) aplicado em um dos quatro cantos da peça. */
@Entity
@Table(name = "cut_plan_item_chamfers")
public class CutPlanItemChamfer {

    public enum Corner { BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "cut_plan_item_id", nullable = false)
    private CutPlanItem cutPlanItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Corner corner;

    /** Comprimento do "perninha" do corte a 45º, em mm. */
    @Column(nullable = false)
    private BigDecimal size;

    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutPlanItem getCutPlanItem() { return cutPlanItem; }
    public void setCutPlanItem(CutPlanItem cutPlanItem) { this.cutPlanItem = cutPlanItem; }
    public Corner getCorner() { return corner; }
    public void setCorner(Corner corner) { this.corner = corner; }
    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
