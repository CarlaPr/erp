package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String number;
    private String title;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;
    private BigDecimal totalValue = BigDecimal.ZERO;
    private LocalDate installDate;

    @ManyToOne
    @JsonIgnoreProperties({
            "quotes",
            "workOrders"
    })
    private Client client;

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnoreProperties("workOrder")
    private List<WorkOrderItem> items = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "quote_id")
    @JsonIgnore
    private Quote quote;

    @Transient
    @JsonProperty("quoteId")
    private UUID quoteId;

    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal area;

    @ManyToOne
    private ServiceCategory category;

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getTotalValue() { return totalValue != null ? totalValue : BigDecimal.ZERO; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public List<WorkOrderItem> getItems() { return items; }
    public void setItems(List<WorkOrderItem> items) { this.items = items; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public UUID getQuoteId() { return (this.quoteId != null) ? this.quoteId : (this.quote != null ? this.quote.getId() : null); }
    public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }

    public BigDecimal getTotalCost() { return items.stream().map(WorkOrderItem::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal getProfit() { return getTotalValue().subtract(getTotalCost()); }
    public String getMargin() { return getTotalValue().compareTo(BigDecimal.ZERO) > 0 ? getProfit().multiply(new BigDecimal("100")).divide(getTotalValue(), 1, java.math.RoundingMode.HALF_UP).toString() : "0"; }

}