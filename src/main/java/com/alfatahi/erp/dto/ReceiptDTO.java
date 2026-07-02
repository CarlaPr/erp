package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO para transferir dados de Receipt entre camadas
 * Contém agregação de dados de Receipt, Client e WorkOrder
 */
public class ReceiptDTO {
    
    private UUID id;
    private String number;
    private UUID accountsReceivableId;
    private UUID workOrderId;
    private UUID clientId;
    private String clientName;
    private String clientDocument;
    private String clientAddress;
    private String workOrderNumber;
    private String workOrderDescription;
    private LocalDate receiptDate;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private String paymentMethod;
    private String status;
    private String responsibleName;
    private String notes;

    // ==================== CONSTRUTORES ====================

    public ReceiptDTO() {
    }

    public ReceiptDTO(UUID id, String number, UUID accountsReceivableId,
                      UUID workOrderId, UUID clientId, String clientName,
                      String clientDocument, String clientAddress,
                      String workOrderNumber, String workOrderDescription,
                      LocalDate receiptDate, BigDecimal totalAmount,
                      BigDecimal receivedAmount, String paymentMethod,
                      String status, String responsibleName, String notes) {
        this.id = id;
        this.number = number;
        this.accountsReceivableId = accountsReceivableId;
        this.workOrderId = workOrderId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientDocument = clientDocument;
        this.clientAddress = clientAddress;
        this.workOrderNumber = workOrderNumber;
        this.workOrderDescription = workOrderDescription;
        this.receiptDate = receiptDate;
        this.totalAmount = totalAmount;
        this.receivedAmount = receivedAmount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.responsibleName = responsibleName;
        this.notes = notes;
    }

    // ==================== GETTERS E SETTERS ====================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public UUID getAccountsReceivableId() {
        return accountsReceivableId;
    }

    public void setAccountsReceivableId(UUID accountsReceivableId) {
        this.accountsReceivableId = accountsReceivableId;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(UUID workOrderId) {
        this.workOrderId = workOrderId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientDocument() {
        return clientDocument;
    }

    public void setClientDocument(String clientDocument) {
        this.clientDocument = clientDocument;
    }

    public String getClientAddress() {
        return clientAddress;
    }

    public void setClientAddress(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public String getWorkOrderDescription() {
        return workOrderDescription;
    }

    public void setWorkOrderDescription(String workOrderDescription) {
        this.workOrderDescription = workOrderDescription;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponsibleName() {
        return responsibleName;
    }

    public void setResponsibleName(String responsibleName) {
        this.responsibleName = responsibleName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "ReceiptDTO{" +
                "id=" + id +
                ", number='" + number + '\'' +
                ", clientName='" + clientName + '\'' +
                ", totalAmount=" + totalAmount +
                ", status='" + status + '\'' +
                '}';
    }
}
