package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DreReportDto {

    private String mesAno;
    private BigDecimal receitaBruta       = BigDecimal.ZERO;
    private BigDecimal cmv                = BigDecimal.ZERO;
    private BigDecimal despesasFixas      = BigDecimal.ZERO;
    private BigDecimal despesasFinanceiras = BigDecimal.ZERO;   // taxas de cartão + tarifas bancárias
    private BigDecimal taxRate = new BigDecimal("0.06");

    public DreReportDto(String mesAno) { this.mesAno = mesAno; }

    public DreReportDto(String mesAno, BigDecimal taxRate) {
        this.mesAno = mesAno;
        this.taxRate = taxRate;
    }

    public String getMesAno() { return mesAno; }

    // ── Impostos (Simples / NF) ──────────────────────────────────────────────
    public BigDecimal getImpostos() {
        return receitaBruta.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Receitas ─────────────────────────────────────────────────────────────
    public BigDecimal getReceitaBruta()  { return receitaBruta; }
    public void addReceita(BigDecimal v) { this.receitaBruta = this.receitaBruta.add(v); }

    public BigDecimal getReceitaLiquida() { return receitaBruta.subtract(getImpostos()); }

    // ── Custos ───────────────────────────────────────────────────────────────
    public BigDecimal getCmv()      { return cmv; }
    public void addCmv(BigDecimal v) { this.cmv = this.cmv.add(v); }

    public BigDecimal getLucroBruto() { return getReceitaLiquida().subtract(cmv); }

    // ── Despesas Fixas ────────────────────────────────────────────────────────
    public BigDecimal getDespesasFixas()       { return despesasFixas; }
    public void addDespesa(BigDecimal v)       { this.despesasFixas = this.despesasFixas.add(v); }

    // ── Despesas Financeiras (taxa de cartão, tarifas) ───────────────────────
    public BigDecimal getDespesasFinanceiras()        { return despesasFinanceiras; }
    public void addDespesaFinanceira(BigDecimal v)    { this.despesasFinanceiras = this.despesasFinanceiras.add(v); }

    // ── Resultado Final ───────────────────────────────────────────────────────
    public BigDecimal getLucroOperacional() {
        return getLucroBruto().subtract(despesasFixas);
    }

    public BigDecimal getLucroLiquido() {
        return getLucroOperacional().subtract(despesasFinanceiras);
    }

    public BigDecimal getMargemLiquida() {
        if (receitaBruta.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getLucroLiquido()
                .multiply(new BigDecimal("100"))
                .divide(receitaBruta, 2, RoundingMode.HALF_UP);
    }
}
