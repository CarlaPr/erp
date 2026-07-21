package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Catálogo de Insumos / Tabela de Preços de Compra.
 * Fonte única de valores usada pelo Plano de Corte para estimar custos.
 * Todo o histórico de alteração de preço é preservado em MaterialPriceHistory.
 */
@Entity
@Table(name = "material_price_items")
public class MaterialPriceItem {

    public enum Category { GLASS, ALUMINUM, HARDWARE, SILICONE, ACCESSORY, OTHER }
    public enum Unit { M2, M, UNIT, TUBE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    private String manufacturer;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Supplier supplier;

    // Campos específicos de VIDRO
    @Column(name = "glass_type")
    private String glassType;
    private BigDecimal thickness;
    private String color;
    private String finish;

    // Campos específicos de ALUMÍNIO
    @Column(name = "aluminum_line")
    private String aluminumLine;
    @Column(name = "aluminum_profile")
    private String aluminumProfile;

    // Campos específicos de FERRAGEM
    @Column(name = "hardware_category")
    private String hardwareCategory;

    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Unit unit;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal price;

    @Column(name = "min_price", precision = 12, scale = 4)
    private BigDecimal minPrice;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    private Boolean active = true;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public String getGlassType() { return glassType; }
    public void setGlassType(String glassType) { this.glassType = glassType; }
    public BigDecimal getThickness() { return thickness; }
    public void setThickness(BigDecimal thickness) { this.thickness = thickness; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getFinish() { return finish; }
    public void setFinish(String finish) { this.finish = finish; }
    public String getAluminumLine() { return aluminumLine; }
    public void setAluminumLine(String aluminumLine) { this.aluminumLine = aluminumLine; }
    public String getAluminumProfile() { return aluminumProfile; }
    public void setAluminumProfile(String aluminumProfile) { this.aluminumProfile = aluminumProfile; }
    public String getHardwareCategory() { return hardwareCategory; }
    public void setHardwareCategory(String hardwareCategory) { this.hardwareCategory = hardwareCategory; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
