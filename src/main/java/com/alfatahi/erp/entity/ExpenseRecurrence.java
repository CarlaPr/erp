package com.alfatahi.erp.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa a "regra" de uma série de Contas a Pagar recorrentes.
 * Cada execução (parcela) é persistida como um registro independente em
 * {@link AccountsPayable}, vinculado por {@code recurrenceId}.
 */
@Entity
@Table(name = "expense_recurrences")
public class ExpenseRecurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String description;

    private String category;
    private String subcategory;

    @Column(name = "expense_type")
    private String expenseType;

    @Column(name = "expense_nature")
    private String expenseNature;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_type", nullable = false, length = 20)
    private RecurrenceEndType endType;

    /** Quantidade total de repetições, quando endType = COUNT. */
    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    /** Data final da recorrência, quando endType = DATE. */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurrenceStatus status = RecurrenceStatus.ACTIVE;

    /** Quantidade de parcelas já geradas até o momento. */
    @Column(name = "total_generated")
    private Integer totalGenerated = 0;

    /** Data de vencimento da última parcela já gerada (usado para "abastecer" recorrências infinitas). */
    @Column(name = "last_generated_due_date")
    private LocalDate lastGeneratedDueDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    public String getExpenseNature() { return expenseNature; }
    public void setExpenseNature(String expenseNature) { this.expenseNature = expenseNature; }
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public RecurrenceFrequency getFrequency() { return frequency; }
    public void setFrequency(RecurrenceFrequency frequency) { this.frequency = frequency; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public RecurrenceEndType getEndType() { return endType; }
    public void setEndType(RecurrenceEndType endType) { this.endType = endType; }
    public Integer getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(Integer occurrenceCount) { this.occurrenceCount = occurrenceCount; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public RecurrenceStatus getStatus() { return status; }
    public void setStatus(RecurrenceStatus status) { this.status = status; }
    public Integer getTotalGenerated() { return totalGenerated != null ? totalGenerated : 0; }
    public void setTotalGenerated(Integer totalGenerated) { this.totalGenerated = totalGenerated; }
    public LocalDate getLastGeneratedDueDate() { return lastGeneratedDueDate; }
    public void setLastGeneratedDueDate(LocalDate lastGeneratedDueDate) { this.lastGeneratedDueDate = lastGeneratedDueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
