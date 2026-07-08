package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "quotes")
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnoreProperties({"quotes", "workOrders"})
    private Client client;

    @Column(nullable = false, length = 50)
    private String status = "pending";

    @Column(name = "total_value", precision = 12, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(columnDefinition = "text")
    private String warranty;

    @Column(name = "payment_method", length = 255)
    private String paymentMethod;

    private Integer installments = 1;

    @Column(name = "date_created")
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Column(name = "date_approved")
    private LocalDateTime dateApproved;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("quote")
    private List<QuoteItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "quote")
    @JsonIgnoreProperties("quote")
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Profile profile;

    @Column(name = "public_token", unique = true, updatable = false)
    private String publicToken = UUID.randomUUID().toString();

    @Column(name = "client_signature", columnDefinition = "text")
    private String clientSignature;

    // Getters e Setters
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }

    public String getClientSignature() { return clientSignature; }
    public void setClientSignature(String clientSignature) { this.clientSignature = clientSignature; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public LocalDateTime getDateApproved() { return dateApproved; }
    public void setDateApproved(LocalDateTime dateApproved) { this.dateApproved = dateApproved; }

    public List<QuoteItem> getItems() { return items; }
    public void setItems(List<QuoteItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
    }
}