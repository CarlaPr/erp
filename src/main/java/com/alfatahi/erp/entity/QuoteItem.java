package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quote_items")
public class QuoteItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "quote_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Quote quote;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String product;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2) private BigDecimal width = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2) private BigDecimal height = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2) private BigDecimal quantity = BigDecimal.ONE;
    @Column(precision = 12, scale = 2) private BigDecimal unitPrice = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "quote_item_measurements", joinColumns = @JoinColumn(name = "quote_item_id"))
    @OrderColumn(name = "measurement_order")
    private List<QuoteItemMeasurement> measurements = new ArrayList<>();

    public List<QuoteItemMeasurement> getMeasurements() { return measurements; }
    public void setMeasurements(List<QuoteItemMeasurement> measurements) {
        this.measurements.clear();
        if (measurements != null) this.measurements.addAll(measurements);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getAreaM2() {
        if (!measurements.isEmpty()) {
            return measurements.stream().map(QuoteItemMeasurement::getAreaM2)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return new QuoteItemMeasurement(width, height).getAreaM2();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getPricingAreaM2() {
        BigDecimal area = getAreaM2();
        return area.signum() > 0 ? area : BigDecimal.ONE;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public boolean hasPerMeasurementPricing() {
        return measurements.stream().anyMatch(measure -> measure.getQuantity() != null || measure.getUnitPrice() != null);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getTotalAreaM2() {
        if (measurements.isEmpty()) return getAreaM2().multiply(getPricingQuantity());
        return measurements.stream().map(measure -> measure.getAreaM2().multiply(measureQuantity(measure)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getSubtotal() {
        if (measurements.isEmpty()) return getPricingAreaM2().multiply(getPricingQuantity()).multiply(priceOrZero(unitPrice));
        return measurements.stream().map(measure -> measure.getAreaM2().multiply(measureQuantity(measure)).multiply(measurePrice(measure)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getPricingQuantity() {
        return hasPerMeasurementPricing() ? BigDecimal.ONE : (quantity != null ? quantity : BigDecimal.ONE);
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public BigDecimal getCalculatedUnitPrice() {
        return hasPerMeasurementPricing() ? getSubtotal() : getPricingAreaM2().multiply(priceOrZero(unitPrice));
    }

    private BigDecimal measureQuantity(QuoteItemMeasurement measure) {
        return measure.getQuantity() != null ? measure.getQuantity() : (quantity != null ? quantity : BigDecimal.ONE);
    }

    private BigDecimal measurePrice(QuoteItemMeasurement measure) {
        return priceOrZero(measure.getUnitPrice() != null ? measure.getUnitPrice() : unitPrice);
    }

    private static BigDecimal priceOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public Quote getQuote() { return quote; } public void setQuote(Quote quote) { this.quote = quote; }
    public String getCategory() { return category; } public void setCategory(String category) { this.category = category; }
    public String getProduct() { return product; } public void setProduct(String product) { this.product = product; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public BigDecimal getWidth() { return width; } public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getHeight() { return height; } public void setHeight(BigDecimal height) { this.height = height; }
    public BigDecimal getQuantity() { return quantity; } public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; } public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}