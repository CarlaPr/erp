package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Linha de custo de Ferragem / Alumínio / Silicone / Outros materiais
 * dentro de um Plano de Corte.
 */
@Entity
@Table(name = "cut_plan_materials")
public class CutPlanMaterial {

    public enum Category { HARDWARE, ALUMINUM, SILICONE, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "cut_plan_id", nullable = false)
    private CutPlan cutPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false)
    private String description;

    private BigDecimal quantity = BigDecimal.ONE;

    private String unit;

    @Column(name = "unit_price_snapshot", precision = 12, scale = 4)
    private BigDecimal unitPriceSnapshot = BigDecimal.ZERO;

    @Column(name = "supplier_name")
    private String supplierName;

    @ManyToOne
    @JoinColumn(name = "price_item_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MaterialPriceItem priceItem;

    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutPlan getCutPlan() { return cutPlan; }
    public void setCutPlan(CutPlan cutPlan) { this.cutPlan = cutPlan; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity != null ? quantity : BigDecimal.ONE; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot != null ? unitPriceSnapshot : BigDecimal.ZERO; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public MaterialPriceItem getPriceItem() { return priceItem; }
    public void setPriceItem(MaterialPriceItem priceItem) { this.priceItem = priceItem; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getTotal() {
        return getQuantity().multiply(getUnitPriceSnapshot()).setScale(2, RoundingMode.HALF_UP);
    }
}
