package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Linha do Livro Caixa.
 * Une entradas (Contas a Receber recebidas) e saídas (Contas a Pagar pagas) em
 * uma visão cronológica com saldo acumulado após cada movimento.
 */
public class CashLedgerEntryDto {

    public enum EntryType { ENTRADA, SAIDA }

    private LocalDate date;
    private EntryType type;
    private String party;           // cliente ou fornecedor
    private String description;
    private String workOrderNumber;
    private String paymentMethod;
    private BigDecimal entrada = BigDecimal.ZERO;
    private BigDecimal saida   = BigDecimal.ZERO;
    private BigDecimal saldo   = BigDecimal.ZERO;
    private String notes;
    private String origin;          // "Recebimento", "Pagamento", "Taxa Cartão"
    private boolean financialExpense;

    public LocalDate getDate()         { return date; }
    public void setDate(LocalDate d)   { this.date = d; }
    public EntryType getType()              { return type; }
    public void setType(EntryType t)        { this.type = t; }
    public String getParty()               { return party; }
    public void setParty(String p)         { this.party = p; }
    public String getDescription()         { return description; }
    public void setDescription(String d)   { this.description = d; }
    public String getWorkOrderNumber()     { return workOrderNumber; }
    public void setWorkOrderNumber(String w) { this.workOrderNumber = w; }
    public String getPaymentMethod()       { return paymentMethod; }
    public void setPaymentMethod(String m) { this.paymentMethod = m; }
    public BigDecimal getEntrada()         { return entrada; }
    public void setEntrada(BigDecimal e)   { this.entrada = e; }
    public BigDecimal getSaida()           { return saida; }
    public void setSaida(BigDecimal s)     { this.saida = s; }
    public BigDecimal getSaldo()           { return saldo; }
    public void setSaldo(BigDecimal s)     { this.saldo = s; }
    public String getNotes()               { return notes; }
    public void setNotes(String n)         { this.notes = n; }
    public String getOrigin()              { return origin; }
    public void setOrigin(String o)        { this.origin = o; }
    public boolean isFinancialExpense()        { return financialExpense; }
    public void setFinancialExpense(boolean b) { this.financialExpense = b; }
}
