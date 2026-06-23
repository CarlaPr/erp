package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class Quote {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false) private String number;
    @ManyToOne @JoinColumn(name = "client_id") private Client client;

    private LocalDate dateCreated = LocalDate.now();
    private LocalDate validityDate;

    private String status = "pending"; // draft, pending, approved, negotiation, cancelled, expired

    @Column(precision = 12, scale = 2) private BigDecimal totalValue = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal discount = BigDecimal.ZERO;

    private String paymentMethod;
    private Integer installments = 1;

    @Column(columnDefinition = "text") private String observations;

    // O VÍNCULO MÁGICO COM A GESTÃO
    @OneToOne @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("quote")
    private List<QuoteItem> items = new ArrayList<>();

    // Getters e Setters para as Datas do Orçamento
    public LocalDate getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDate dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDate getValidityDate() {
        return validityDate;
    }

    public void setValidityDate(LocalDate validityDate) {
        this.validityDate = validityDate;
    }


    @Column(columnDefinition = "text")
    private String warranty;

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public String getNumber() { return number; } public void setNumber(String number) { this.number = number; }
    public Client getClient() { return client; } public void setClient(Client client) { this.client = client; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalValue() { return totalValue; } public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public WorkOrder getWorkOrder() { return workOrder; } public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public List<QuoteItem> getItems() { return items; } public void setItems(List<QuoteItem> items) { this.items.clear(); if(items != null) this.items.addAll(items); }
    public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String p) { this.paymentMethod = p; }
    public Integer getInstallments() { return installments; } public void setInstallments(Integer i) { this.installments = i; }
    public String getObservations() { return observations; } public void setObservations(String o) { this.observations = o; }
}