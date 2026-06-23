package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String number;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "varchar(30) default 'budget'")
    private String status = "budget";

    @Column(name = "total_value", precision = 12, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "install_address")
    private String installAddress;

    @Column(name = "install_city")
    private String installCity;

    @Column(name = "install_date")
    private LocalDate installDate;

    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal area;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public ServiceCategory getCategory() { return category; }
    public void setCategory(ServiceCategory category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public String getInstallAddress() { return installAddress; }
    public void setInstallAddress(String installAddress) { this.installAddress = installAddress; }
    public String getInstallCity() { return installCity; }
    public void setInstallCity(String installCity) { this.installCity = installCity; }
    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }
    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }


    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<WorkOrderItem> items = new java.util.ArrayList<>();

    public java.util.List<WorkOrderItem> getItems() { return items; }
    public void setItems(java.util.List<WorkOrderItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
    }

    // Métodos utilitários para o Dashboard e Tabelas
    public BigDecimal getTotalCost() {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        return items.stream()
                .map(WorkOrderItem::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getProfit() {
        return (totalValue != null ? totalValue : BigDecimal.ZERO).subtract(getTotalCost());
    }

    public BigDecimal getMargin() {
        BigDecimal rev = (totalValue != null ? totalValue : BigDecimal.ZERO);
        if (rev.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getProfit().multiply(new BigDecimal("100")).divide(rev, 2, java.math.RoundingMode.HALF_UP);
    }
}