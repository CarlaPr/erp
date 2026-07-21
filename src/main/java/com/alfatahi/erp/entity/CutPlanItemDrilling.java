package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Furo posicionado em uma peça de vidro do Plano de Corte.
 * Posição (posX, posY) é medida em mm a partir do canto inferior esquerdo
 * da peça (vista frontal), usada para gerar o desenho técnico.
 */
@Entity
@Table(name = "cut_plan_item_drillings")
public class CutPlanItemDrilling {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "cut_plan_item_id", nullable = false)
    private CutPlanItem cutPlanItem;

    @Column(name = "pos_x", nullable = false)
    private BigDecimal posX;

    @Column(name = "pos_y", nullable = false)
    private BigDecimal posY;

    @Column(nullable = false)
    private BigDecimal diameter;

    @Column(name = "drill_type")
    private String drillType = "passante"; // passante, escareado, rosca... (livre, sem valor fixo em regra de negócio)

    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutPlanItem getCutPlanItem() { return cutPlanItem; }
    public void setCutPlanItem(CutPlanItem cutPlanItem) { this.cutPlanItem = cutPlanItem; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
    public BigDecimal getDiameter() { return diameter; }
    public void setDiameter(BigDecimal diameter) { this.diameter = diameter; }
    public String getDrillType() { return drillType; }
    public void setDrillType(String drillType) { this.drillType = drillType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
