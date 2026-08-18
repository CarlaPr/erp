package com.alfatahi.erp.entity;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "technical_visit_openings")
public class TechnicalVisitOpening {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technical_visit_id", nullable = false)
    @JsonIgnore
    private TechnicalVisit technicalVisit;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_category", length = 40)
    private CategoriaServico serviceCategory;

    @Column(name = "width_mm", precision = 9, scale = 2)
    private BigDecimal widthMm;

    @Column(name = "height_mm", precision = 9, scale = 2)
    private BigDecimal heightMm;

    @Column(name = "gross_height_left_mm", precision = 9, scale = 2)
    private BigDecimal grossHeightLeftMm;

    @Column(name = "gross_height_right_mm", precision = 9, scale = 2)
    private BigDecimal grossHeightRightMm;

    @Column(name = "gross_width_top_mm", precision = 9, scale = 2)
    private BigDecimal grossWidthTopMm;

    @Column(name = "gross_width_bottom_mm", precision = 9, scale = 2)
    private BigDecimal grossWidthBottomMm;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "opening", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private List<TechnicalVisitFeature> features = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public TechnicalVisit getTechnicalVisit() { return technicalVisit; }
    public void setTechnicalVisit(TechnicalVisit technicalVisit) { this.technicalVisit = technicalVisit; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public CategoriaServico getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(CategoriaServico value) { this.serviceCategory = value; }
    public BigDecimal getWidthMm() { return widthMm; }
    public void setWidthMm(BigDecimal widthMm) { this.widthMm = widthMm; }
    public BigDecimal getHeightMm() { return heightMm; }
    public void setHeightMm(BigDecimal heightMm) { this.heightMm = heightMm; }
    public BigDecimal getGrossHeightLeftMm() { return grossHeightLeftMm; }
    public void setGrossHeightLeftMm(BigDecimal value) { this.grossHeightLeftMm = value; }
    public BigDecimal getGrossHeightRightMm() { return grossHeightRightMm; }
    public void setGrossHeightRightMm(BigDecimal value) { this.grossHeightRightMm = value; }
    public BigDecimal getGrossWidthTopMm() { return grossWidthTopMm; }
    public void setGrossWidthTopMm(BigDecimal value) { this.grossWidthTopMm = value; }
    public BigDecimal getGrossWidthBottomMm() { return grossWidthBottomMm; }
    public void setGrossWidthBottomMm(BigDecimal value) { this.grossWidthBottomMm = value; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<TechnicalVisitFeature> getFeatures() { return features; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
