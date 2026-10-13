package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "expense_allocations")
public class ExpenseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "accounts_payable_id", nullable = false)
    private AccountsPayable accountsPayable;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal percentage = BigDecimal.ZERO;

    @Column(name = "allocation_value", precision = 12, scale = 2)
    private BigDecimal value;

     @Column(name = "description")
    private String description;

   @OneToOne
    @JoinColumn(name = "work_order_item_id")
    private WorkOrderItem workOrderItem;

    public ExpenseAllocation() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public AccountsPayable getAccountsPayable() { return accountsPayable; }
    public void setAccountsPayable(AccountsPayable accountsPayable) { this.accountsPayable = accountsPayable; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public WorkOrderItem getWorkOrderItem() { return workOrderItem; }
    public void setWorkOrderItem(WorkOrderItem workOrderItem) { this.workOrderItem = workOrderItem; }
}
