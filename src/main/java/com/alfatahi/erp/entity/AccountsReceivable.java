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

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Column(nullable = false)
    private String description;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "received_amount", precision = 12, scale = 2)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Column(name = "discount", precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(nullable = false)
    private String status = "pending"; // pending | partial | received | cancelled

    // REGRA DE NEGÓCIO (A1): "Recebido" (empresa confirmou) e "Conciliado" (banco
    // confirmou) são coisas diferentes. Este campo é independente do "status" acima
    // e representa só o lado do banco: NAO_CONCILIADO | CONCILIADO | DIVERGENTE.
    // Ex.: um título pode estar status=received e reconciliationStatus=NAO_CONCILIADO
    // até o extrato bancário confirmar (ou não) aquele valor.
    @Column(name = "reconciliation_status")
    private String reconciliationStatus = "NAO_CONCILIADO";

    @Column(name = "payment_method")
    private String paymentMethod;

    private Integer installments = 1;

    @Column(name = "card_fee_percentage", precision = 5, scale = 2)
    private BigDecimal cardFeePercentage = BigDecimal.ZERO;

    // REGRA DE NEGÓCIO (A2): valor_taxa acumulado — quanto do total já "recebido"
    // (receivedAmount) corresponde a taxa de cartão (não é dinheiro que entrou,
    // é custo da operação). Mantido separado para nunca perder essa informação,
    // mesmo quando o saldo da conta já foi totalmente liquidado.
    @Column(name = "fee_amount", precision = 12, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount != null ? receivedAmount : BigDecimal.ZERO; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getDiscount() { return discount != null ? discount : BigDecimal.ZERO; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReconciliationStatus() { return reconciliationStatus != null ? reconciliationStatus : "NAO_CONCILIADO"; }
    public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }
    public BigDecimal getCardFeePercentage() { return cardFeePercentage; }
    public void setCardFeePercentage(BigDecimal cardFeePercentage) { this.cardFeePercentage = cardFeePercentage; }
    public BigDecimal getFeeAmount() { return feeAmount != null ? feeAmount : BigDecimal.ZERO; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }


    // Valor líquido efetivamente recebido (bruto - taxa de maquininha)
    public BigDecimal getNetReceivedAmount() {
        return getReceivedAmount().subtract(getFeeAmount());
    }

    // Saldo correto: total da OS menos o valor bruto abatido (= o que ainda falta receber)
    // O receivedAmount já é o valor bruto (OS), então o saldo é totalAmount - receivedAmount.
    // Para exibição do "quanto entrou no caixa" use getNetReceivedAmount().
    public BigDecimal getBalance() {
        return totalAmount.subtract(getReceivedAmount());
    }
}
