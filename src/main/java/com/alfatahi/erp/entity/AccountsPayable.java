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

    /** Tipo da despesa: FIXA ou VARIAVEL — usado para relatórios de CMV/custo fixo. */
    @Column(name = "expense_type", length = 20)
    private String expenseType;

    /** Natureza da despesa: OPERACIONAL, ADMINISTRATIVA, FINANCEIRA, TRIBUTARIA, PESSOAL, INVESTIMENTO. */
    @Column(name = "expense_nature", length = 30)
    private String expenseNature;

    /** Centro de custo (texto livre, ex.: Administrativo, Produção, Comercial, ou nº da O.S.). */
    @Column(name = "cost_center", length = 120)
    private String costCenter;

    /** Competência contábil (mês/ano de referência da despesa, independente do vencimento). */
    @Column(name = "competencia")
    private LocalDate competencia;

    /** Identificador da série de recorrência (nulo para lançamentos avulsos). */
    @Column(name = "recurrence_id")
    private UUID recurrenceId;

    /** Posição desta parcela dentro da série de recorrência (1, 2, 3...). */
    @Column(name = "recurrence_seq")
    private Integer recurrenceSeq;

    public Boolean getFinancialExpense() { return financialExpense != null ? financialExpense : false; }
    public void setFinancialExpense(Boolean financialExpense) { this.financialExpense = financialExpense; }
    public UUID getSourceReceivableId() { return sourceReceivableId; }
    public void setSourceReceivableId(UUID sourceReceivableId) { this.sourceReceivableId = sourceReceivableId; }
    public String getExpenseType() { return expenseType; }
    public void setExpenseType(String expenseType) { this.expenseType = expenseType; }
    public String getExpenseNature() { return expenseNature; }
    public void setExpenseNature(String expenseNature) { this.expenseNature = expenseNature; }
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    public LocalDate getCompetencia() { return competencia; }
    public void setCompetencia(LocalDate competencia) { this.competencia = competencia; }
    public UUID getRecurrenceId() { return recurrenceId; }
    public void setRecurrenceId(UUID recurrenceId) { this.recurrenceId = recurrenceId; }
    public Integer getRecurrenceSeq() { return recurrenceSeq; }
    public void setRecurrenceSeq(Integer recurrenceSeq) { this.recurrenceSeq = recurrenceSeq; }

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

    /** Verdadeiro quando a conta está pendente/parcial e o vencimento já passou. */
    public boolean isOverdue() {
        return dueDate != null
                && ("pending".equals(status) || "partial".equals(status))
                && dueDate.isBefore(LocalDate.now());
    }
}
