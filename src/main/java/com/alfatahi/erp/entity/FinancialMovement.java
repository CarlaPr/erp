package com.alfatahi.erp.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "financial_movements")
public class FinancialMovement {

    public enum MovementType { ENTRADA, SAIDA }
    public enum BalanceLocation { BANK, CASH }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_location", nullable = false, length = 10)
    private BalanceLocation balanceLocation;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "payment_method")
    private String paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_receivable_id")
    private AccountsReceivable accountsReceivable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_payable_id")
    private AccountsPayable accountsPayable;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public LocalDate getMovementDate() { return movementDate; }
    public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }
    public BalanceLocation getBalanceLocation() { return balanceLocation; }
    public void setBalanceLocation(BalanceLocation balanceLocation) { this.balanceLocation = balanceLocation; }
    public BigDecimal getAmount() { return amount != null ? amount : BigDecimal.ZERO; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public AccountsReceivable getAccountsReceivable() { return accountsReceivable; }
    public void setAccountsReceivable(AccountsReceivable accountsReceivable) { this.accountsReceivable = accountsReceivable; }
    public AccountsPayable getAccountsPayable() { return accountsPayable; }
    public void setAccountsPayable(AccountsPayable accountsPayable) { this.accountsPayable = accountsPayable; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
