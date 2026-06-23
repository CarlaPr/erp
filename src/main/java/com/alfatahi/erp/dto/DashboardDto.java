package com.alfatahi.erp.dto;

import java.math.BigDecimal;

public class DashboardDto {
    private BigDecimal saldoAtual = BigDecimal.ZERO;
    private BigDecimal saldoProjetado = BigDecimal.ZERO;

    private BigDecimal aReceber = BigDecimal.ZERO;
    private BigDecimal recebido = BigDecimal.ZERO;

    private BigDecimal aPagar = BigDecimal.ZERO;
    private BigDecimal pago = BigDecimal.ZERO;

    private BigDecimal lucroBruto = BigDecimal.ZERO;
    private BigDecimal margemMedia = BigDecimal.ZERO;

    private BigDecimal ticketMedio = BigDecimal.ZERO;
    private Long osAtivas = 0L;

    private Long inadimplentes = 0L;

    private BigDecimal perdas = BigDecimal.ZERO;
    private BigDecimal perdasPercentual = BigDecimal.ZERO;

    // Getters e Setters
    public BigDecimal getSaldoAtual() { return saldoAtual; }
    public void setSaldoAtual(BigDecimal saldoAtual) { this.saldoAtual = saldoAtual; }
    public BigDecimal getSaldoProjetado() { return saldoProjetado; }
    public void setSaldoProjetado(BigDecimal saldoProjetado) { this.saldoProjetado = saldoProjetado; }
    public BigDecimal getaReceber() { return aReceber; }
    public void setaReceber(BigDecimal aReceber) { this.aReceber = aReceber; }
    public BigDecimal getRecebido() { return recebido; }
    public void setRecebido(BigDecimal recebido) { this.recebido = recebido; }
    public BigDecimal getaPagar() { return aPagar; }
    public void setaPagar(BigDecimal aPagar) { this.aPagar = aPagar; }
    public BigDecimal getPago() { return pago; }
    public void setPago(BigDecimal pago) { this.pago = pago; }
    public BigDecimal getLucroBruto() { return lucroBruto; }
    public void setLucroBruto(BigDecimal lucroBruto) { this.lucroBruto = lucroBruto; }
    public BigDecimal getMargemMedia() { return margemMedia; }
    public void setMargemMedia(BigDecimal margemMedia) { this.margemMedia = margemMedia; }
    public BigDecimal getTicketMedio() { return ticketMedio; }
    public void setTicketMedio(BigDecimal ticketMedio) { this.ticketMedio = ticketMedio; }
    public Long getOsAtivas() { return osAtivas; }
    public void setOsAtivas(Long osAtivas) { this.osAtivas = osAtivas; }
    public Long getInadimplentes() { return inadimplentes; }
    public void setInadimplentes(Long inadimplentes) { this.inadimplentes = inadimplentes; }
    public BigDecimal getPerdas() { return perdas; }
    public void setPerdas(BigDecimal perdas) { this.perdas = perdas; }
    public BigDecimal getPerdasPercentual() { return perdasPercentual; }
    public void setPerdasPercentual(BigDecimal perdasPercentual) { this.perdasPercentual = perdasPercentual; }
}