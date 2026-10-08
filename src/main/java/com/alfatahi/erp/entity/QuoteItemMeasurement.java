package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class QuoteItemMeasurement {

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public QuoteItemMeasurement() {
    }

    public QuoteItemMeasurement(BigDecimal width, BigDecimal height) {
        this.width = width;
        this.height = height;
    }

    public QuoteItemMeasurement(BigDecimal width, BigDecimal height, BigDecimal quantity, BigDecimal unitPrice) {
        this(width, height);
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    @JsonIgnore
    public BigDecimal getAreaM2() {
        return width != null && height != null && width.signum() > 0 && height.signum() > 0
                ? width.multiply(height) : BigDecimal.ZERO;
    }
}
