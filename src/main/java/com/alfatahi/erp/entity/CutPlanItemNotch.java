package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Recorte retangular posicionado em uma peça de vidro (ex: recorte para
 * tomada, dobradiça embutida, coluna). Posição do canto inferior esquerdo
 * do recorte (posX, posY) em mm, a partir do canto inferior esquerdo da peça.
 */
@Entity
@Table(name = "cut_plan_item_notches")
public class CutPlanItemNotch {

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
    private BigDecimal width;

    @Column(nullable = false)
    private BigDecimal height;

    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutPlanItem getCutPlanItem() { return cutPlanItem; }
    public void setCutPlanItem(CutPlanItem cutPlanItem) { this.cutPlanItem = cutPlanItem; }
    public BigDecimal getPosX() { return posX; }
    public void setPosX(BigDecimal posX) { this.posX = posX; }
    public BigDecimal getPosY() { return posY; }
    public void setPosY(BigDecimal posY) { this.posY = posY; }
    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
