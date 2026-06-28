package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @ManyToOne
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(nullable = false)
    private String type; // "IN" (Entrada) ou "OUT" (Saída)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = "pending"; // "pending" (Apenas no ERP) ou "conciliated" (Confirmado no Banco)

    // CORREÇÃO (A4): identificador único do lançamento no extrato (FITID do OFX).
    // Usado para não deixar reimportar o mesmo extrato e duplicar lançamentos/baixas.
    @Column(name = "external_id")
    private String externalId;

    // REGRA DE NEGÓCIO (A1): vínculo persistido entre o lançamento bancário e o
    // título que ele liquidou — antes não existia, e era impossível auditar depois
    // "qual lançamento pagou qual título". Só um dos dois é preenchido por transação.
    @ManyToOne
    @JoinColumn(name = "matched_receivable_id")
    private AccountsReceivable matchedReceivable;

    @ManyToOne
    @JoinColumn(name = "matched_payable_id")
    private AccountsPayable matchedPayable;

    // Diferença entre o valor esperado (líquido, já considerando taxa de cartão
    // informada) e o valor que realmente apareceu no extrato. Zero quando concilia
    // sem divergência.
    @Column(name = "divergence_amount", precision = 12, scale = 2)
    private BigDecimal divergenceAmount = BigDecimal.ZERO;

    // Motivo da conciliação/divergência (ex.: "Taxa cartão 5% (R$ 500,00)" ou
    // "Divergência de R$ 1.500,00 — verificar lançamento").
    @Column(name = "reconciliation_note", columnDefinition = "TEXT")
    private String reconciliationNote;

    // ==========================================
    // Getters e Setters
    // ==========================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BankAccount getBankAccount() { return bankAccount; }
    public void setBankAccount(BankAccount bankAccount) { this.bankAccount = bankAccount; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public AccountsReceivable getMatchedReceivable() { return matchedReceivable; }
    public void setMatchedReceivable(AccountsReceivable matchedReceivable) { this.matchedReceivable = matchedReceivable; }

    public AccountsPayable getMatchedPayable() { return matchedPayable; }
    public void setMatchedPayable(AccountsPayable matchedPayable) { this.matchedPayable = matchedPayable; }

    public BigDecimal getDivergenceAmount() { return divergenceAmount != null ? divergenceAmount : BigDecimal.ZERO; }
    public void setDivergenceAmount(BigDecimal divergenceAmount) { this.divergenceAmount = divergenceAmount; }

    public String getReconciliationNote() { return reconciliationNote; }
    public void setReconciliationNote(String reconciliationNote) { this.reconciliationNote = reconciliationNote; }

    public boolean isDivergente() {
        return divergenceAmount != null && divergenceAmount.compareTo(BigDecimal.ZERO) != 0;
    }
}