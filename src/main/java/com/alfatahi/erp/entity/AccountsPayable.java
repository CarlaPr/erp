package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts_payable")
public class AccountsPayable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @OneToMany(
            mappedBy = "accountsPayable",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private List<ExpenseAllocation> allocations = new ArrayList<>();

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "subcategory")
    private String subcategory;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;
    @Column(nullable = false)
    private String status = "pending";

    @Column(name = "reconciliation_status")
    private String reconciliationStatus = "NAO_CONCILIADO";

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "is_recurring")
    private Boolean recurring = false;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_financial_expense", nullable = false)
    private Boolean financialExpense = false;

    @Column(name = "source_receivable_id")
    private UUID sourceReceivableId;

    public Boolean getFinancialExpense() { return financialExpense != null ? financialExpense : false; }
    public void setFinancialExpense(Boolean financialExpense) { this.financialExpense = financialExpense; }
    public UUID getSourceReceivableId() { return sourceReceivableId; }
    public void setSourceReceivableId(UUID sourceReceivableId) { this.sourceReceivableId = sourceReceivableId; }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public List<ExpenseAllocation> getAllocations() { return allocations; }
    public void setAllocations(List<ExpenseAllocation> allocations) { this.allocations = allocations; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getPaidAmount() { return paidAmount != null ? paidAmount : BigDecimal.ZERO; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
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
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public Boolean getRecurring() { return recurring; }
    public void setRecurring(Boolean recurring) { this.recurring = recurring; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getBalance() {
        return totalAmount.subtract(getPaidAmount());
    }
}
