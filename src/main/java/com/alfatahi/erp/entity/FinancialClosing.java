package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_closings")
public class FinancialClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "opening_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "total_in", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIn = BigDecimal.ZERO;

    @Column(name = "total_out", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOut = BigDecimal.ZERO;

    @Column(name = "pending_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Column(name = "received_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Column(name = "financial_expenses", nullable = false, precision = 12, scale = 2)
    private BigDecimal financialExpenses = BigDecimal.ZERO;

    @Column(name = "net_profit", nullable = false, precision = 12, scale = 2)
    private BigDecimal netProfit = BigDecimal.ZERO;

    @Column(name = "closing_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt = LocalDateTime.now();

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getTotalIn() { return totalIn; }
    public void setTotalIn(BigDecimal totalIn) { this.totalIn = totalIn; }
    public BigDecimal getTotalOut() { return totalOut; }
    public void setTotalOut(BigDecimal totalOut) { this.totalOut = totalOut; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(BigDecimal receivedAmount) { this.receivedAmount = receivedAmount; }
    public BigDecimal getFinancialExpenses() { return financialExpenses; }
    public void setFinancialExpenses(BigDecimal financialExpenses) { this.financialExpenses = financialExpenses; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
