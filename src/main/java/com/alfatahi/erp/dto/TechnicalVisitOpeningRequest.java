package com.alfatahi.erp.dto;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.TipoElemento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TechnicalVisitOpeningRequest {
    private UUID id;
    private CategoriaServico serviceCategory;
    private String name;
    private BigDecimal widthMm;
    private BigDecimal heightMm;
    private BigDecimal grossHeightLeftMm;
    private BigDecimal grossHeightRightMm;
    private BigDecimal grossWidthTopMm;
    private BigDecimal grossWidthBottomMm;
    private String notes;
    private List<FeatureRequest> features = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CategoriaServico getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(CategoriaServico value) { this.serviceCategory = value; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getWidthMm() { return widthMm; }
    public void setWidthMm(BigDecimal value) { this.widthMm = value; }
    public BigDecimal getHeightMm() { return heightMm; }
    public void setHeightMm(BigDecimal value) { this.heightMm = value; }
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
    public List<FeatureRequest> getFeatures() { return features; }
    public void setFeatures(List<FeatureRequest> value) { this.features = value != null ? value : new ArrayList<>(); }

    public static class FeatureRequest {
        private TipoElemento type;
        private String name;
        private ReferenciaHorizontal referenceHorizontal = ReferenciaHorizontal.ESQUERDA;
        private BigDecimal distanceHorizontalMm;
        private ReferenciaVertical referenceVertical = ReferenciaVertical.SUPERIOR;
        private BigDecimal distanceVerticalMm;
        private BigDecimal diameterMm;
        private BigDecimal widthMm;
        private BigDecimal heightMm;
        private BigDecimal depthMm;
        private BigDecimal radiusMm;
        private String corner;
        private String notes;

        public TipoElemento getType() { return type; }
        public void setType(TipoElemento value) { this.type = value; }
        public String getName() { return name; }
        public void setName(String value) { this.name = value; }
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
        public void setCorner(String value) { this.corner = value; }
        public String getNotes() { return notes; }
        public void setNotes(String value) { this.notes = value; }
    }
}
