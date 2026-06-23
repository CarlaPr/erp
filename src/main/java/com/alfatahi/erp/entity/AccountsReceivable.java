package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "accounts_receivable")
public class AccountsReceivable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(nullable = false)
    private String description;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String status = "pending"; // pending, received, cancelled

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder; // Vínculo opcional com a O.S.

    @Column(name = "received_amount", precision = 12, scale = 2)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    private Integer installments = 1; // Número de parcelas

    @Column(name = "card_fee_percentage", precision = 5, scale = 2)
    private BigDecimal cardFeePercentage = BigDecimal.ZERO; // Taxa do cartão

    // Adicione os Getters e Setters para estes 4 novos campos!
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }
    public BigDecimal getCardFeePercentage() { return cardFeePercentage; }
    public void setCardFeePercentage(BigDecimal cardFeePercentage) { this.cardFeePercentage = cardFeePercentage; }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}