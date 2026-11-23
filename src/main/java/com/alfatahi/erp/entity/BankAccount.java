package com.alfatahi.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "opening_balance", precision = 14, scale = 2)
    private BigDecimal openingBalance;

    protected BankAccount() {
    }

    public BankAccount(String name, BigDecimal openingBalance) {
        this.name = name;
        this.openingBalance = openingBalance;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
}
