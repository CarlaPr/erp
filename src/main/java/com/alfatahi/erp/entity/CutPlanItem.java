package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Uma peça de vidro dentro do Plano de Corte.
 * gross* = medida do vão/abertura informada. final* = medida real de corte
 * do vidro, já com os descontos técnicos aplicados (ver CutRuleSet).
 */
@Entity
@Table(name = "cut_plan_items")
public class CutPlanItem {

    // Densidade média do vidro float (kg por m² por mm de espessura),
    // usada apenas para estimar peso: 2.5 g/cm³ é o valor físico padrão do vidro.
    public static final BigDecimal GLASS_DENSITY_KG_PER_M2_PER_MM = new BigDecimal("2.5");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "cut_plan_id", nullable = false)
    private CutPlan cutPlan;

    private String description;
    private String environment;

    @Column(name = "glass_type")
    private String glassType;
    private BigDecimal thickness;
    private String color;
    private String finish;

    @Column(name = "gross_width")
    private BigDecimal grossWidth;
    @Column(name = "gross_height")
    private BigDecimal grossHeight;
    @Column(name = "final_width")
    private BigDecimal finalWidth;
    @Column(name = "final_height")
    private BigDecimal finalHeight;

    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "angle_type")
    private String angleType; // "90", "45", "esquadria_reta", "esquadria_especial"

    @Column(name = "edge_work")
    private String edgeWork; // lapidado, bisotado, boleado, chanfrado...

    @Column(name = "drilling_count")
    private Integer drillingCount = 0;
    @Column(name = "notch_count")
    private Integer notchCount = 0;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "price_item_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MaterialPriceItem priceItem;

    @Column(name = "unit_price_snapshot", precision = 12, scale = 4)
    private BigDecimal unitPriceSnapshot = BigDecimal.ZERO; // valor do m² no momento em que a peça foi adicionada

    private String observations;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "cutPlanItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cutPlanItem")
    private List<CutPlanItemDrilling> drillings = new ArrayList<>();

    @OneToMany(mappedBy = "cutPlanItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cutPlanItem")
    private List<CutPlanItemNotch> notches = new ArrayList<>();

    @OneToMany(mappedBy = "cutPlanItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("cutPlanItem")
    private List<CutPlanItemChamfer> chamfers = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutPlan getCutPlan() { return cutPlan; }
    public void setCutPlan(CutPlan cutPlan) { this.cutPlan = cutPlan; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getGlassType() { return glassType; }
    public void setGlassType(String glassType) { this.glassType = glassType; }
    public BigDecimal getThickness() { return thickness; }
    public void setThickness(BigDecimal thickness) { this.thickness = thickness; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getFinish() { return finish; }
    public void setFinish(String finish) { this.finish = finish; }
    public BigDecimal getGrossWidth() { return grossWidth; }
    public void setGrossWidth(BigDecimal grossWidth) { this.grossWidth = grossWidth; }
    public BigDecimal getGrossHeight() { return grossHeight; }
    public void setGrossHeight(BigDecimal grossHeight) { this.grossHeight = grossHeight; }
    public BigDecimal getFinalWidth() { return finalWidth; }
    public void setFinalWidth(BigDecimal finalWidth) { this.finalWidth = finalWidth; }
    public BigDecimal getFinalHeight() { return finalHeight; }
    public void setFinalHeight(BigDecimal finalHeight) { this.finalHeight = finalHeight; }
    public BigDecimal getQuantity() { return quantity != null ? quantity : BigDecimal.ONE; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getAngleType() { return angleType; }
    public void setAngleType(String angleType) { this.angleType = angleType; }
    public String getEdgeWork() { return edgeWork; }
    public void setEdgeWork(String edgeWork) { this.edgeWork = edgeWork; }
    public Integer getDrillingCount() { return !drillings.isEmpty() ? drillings.size() : drillingCount; }
    public void setDrillingCount(Integer drillingCount) { this.drillingCount = drillingCount; }
    public Integer getNotchCount() { return !notches.isEmpty() ? notches.size() : notchCount; }
    public void setNotchCount(Integer notchCount) { this.notchCount = notchCount; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public MaterialPriceItem getPriceItem() { return priceItem; }
    public void setPriceItem(MaterialPriceItem priceItem) { this.priceItem = priceItem; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot != null ? unitPriceSnapshot : BigDecimal.ZERO; }
    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public List<CutPlanItemDrilling> getDrillings() { return drillings; }
    public void setDrillings(List<CutPlanItemDrilling> drillings) {
        this.drillings.clear();
        if (drillings != null) this.drillings.addAll(drillings);
    }
    public List<CutPlanItemNotch> getNotches() { return notches; }
    public void setNotches(List<CutPlanItemNotch> notches) {
        this.notches.clear();
        if (notches != null) this.notches.addAll(notches);
    }
    public List<CutPlanItemChamfer> getChamfers() { return chamfers; }
    public void setChamfers(List<CutPlanItemChamfer> chamfers) {
        this.chamfers.clear();
        if (chamfers != null) this.chamfers.addAll(chamfers);
    }

    // ── Cálculos derivados ──

    /** Área de UMA peça, em m². */
    public BigDecimal getUnitArea() {
        if (finalWidth == null || finalHeight == null) return BigDecimal.ZERO;
        return finalWidth.multiply(finalHeight)
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);
    }

    /** Área total considerando a quantidade de peças. */
    public BigDecimal getArea() {
        return getUnitArea().multiply(getQuantity());
    }

    /** Peso estimado (kg) = área total x espessura(mm) x densidade do vidro. */
    public BigDecimal getWeight() {
        if (thickness == null) return BigDecimal.ZERO;
        return getArea().multiply(thickness).multiply(GLASS_DENSITY_KG_PER_M2_PER_MM)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalCost() {
        return getArea().multiply(getUnitPriceSnapshot()).setScale(2, RoundingMode.HALF_UP);
    }
}
