package com.alfatahi.erp.entity;

import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "technical_visit_features")
public class TechnicalVisitFeature {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opening_id", nullable = false)
    @JsonIgnore
    private TechnicalVisitOpening opening;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TipoElemento type;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_horizontal", nullable = false, length = 20)
    private ReferenciaHorizontal referenceHorizontal = ReferenciaHorizontal.ESQUERDA;

    @Column(name = "distance_horizontal_mm", precision = 9, scale = 2)
    private BigDecimal distanceHorizontalMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_vertical", nullable = false, length = 20)
    private ReferenciaVertical referenceVertical = ReferenciaVertical.SUPERIOR;

    @Column(name = "distance_vertical_mm", precision = 9, scale = 2)
    private BigDecimal distanceVerticalMm;

    @Column(name = "diameter_mm", precision = 9, scale = 2)
    private BigDecimal diameterMm;
    @Column(name = "width_mm", precision = 9, scale = 2)
    private BigDecimal widthMm;
    @Column(name = "height_mm", precision = 9, scale = 2)
    private BigDecimal heightMm;
    @Column(name = "depth_mm", precision = 9, scale = 2)
    private BigDecimal depthMm;
    @Column(name = "radius_mm", precision = 9, scale = 2)
    private BigDecimal radiusMm;
    @Column(length = 40)
    private String corner;
    @Column(length = 255)
    private String notes;

    public UUID getId() { return id; }
    public TechnicalVisitOpening getOpening() { return opening; }
    public void setOpening(TechnicalVisitOpening opening) { this.opening = opening; }
    public TipoElemento getType() { return type; }
    public void setType(TipoElemento type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ReferenciaHorizontal getReferenceHorizontal() { return referenceHorizontal; }
    public void setReferenceHorizontal(ReferenciaHorizontal value) { this.referenceHorizontal = value; }
    public BigDecimal getDistanceHorizontalMm() { return distanceHorizontalMm; }
    public void setDistanceHorizontalMm(BigDecimal value) { this.distanceHorizontalMm = value; }
    public ReferenciaVertical getReferenceVertical() { return referenceVertical; }
    public void setReferenceVertical(ReferenciaVertical value) { this.referenceVertical = value; }
    public BigDecimal getDistanceVerticalMm() { return distanceVerticalMm; }
    public void setDistanceVerticalMm(BigDecimal value) { this.distanceVerticalMm = value; }
    public BigDecimal getDiameterMm() { return diameterMm; }
    public void setDiameterMm(BigDecimal value) { this.diameterMm = value; }
    public BigDecimal getWidthMm() { return widthMm; }
    public void setWidthMm(BigDecimal value) { this.widthMm = value; }
    public BigDecimal getHeightMm() { return heightMm; }
    public void setHeightMm(BigDecimal value) { this.heightMm = value; }
    public BigDecimal getDepthMm() { return depthMm; }
    public void setDepthMm(BigDecimal value) { this.depthMm = value; }
    public BigDecimal getRadiusMm() { return radiusMm; }
    public void setRadiusMm(BigDecimal value) { this.radiusMm = value; }
    public String getCorner() { return corner; }
    public void setCorner(String corner) { this.corner = corner; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
