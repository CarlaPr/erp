package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "quote_items")
public class QuoteItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne @JoinColumn(name = "quote_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Quote quote;

    private String category; // Box, Sacada, Janela, Espelho
    private String product;
    private String description;

    @Column(precision = 10, scale = 2) private BigDecimal width = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2) private BigDecimal height = BigDecimal.ZERO;
    @Column(precision = 10, scale = 2) private BigDecimal quantity = BigDecimal.ONE;
    @Column(precision = 12, scale = 2) private BigDecimal unitPrice = BigDecimal.ZERO;

    // Gere os Getters e Setters padrão
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