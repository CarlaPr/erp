package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DreReportDto {
    private String mesAno;
    private BigDecimal receitaBruta = BigDecimal.ZERO;
    private BigDecimal cmv = BigDecimal.ZERO;
    private BigDecimal despesasFixas = BigDecimal.ZERO;
    private BigDecimal taxRate = new BigDecimal("0.06");

    public DreReportDto(String mesAno) { this.mesAno = mesAno; }

    public String getMesAno() { return mesAno; }

    public DreReportDto(String mesAno, BigDecimal taxRate) {
        this.mesAno = mesAno;
        this.taxRate = taxRate;
    }

    public BigDecimal getImpostos() {
        return receitaBruta.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getReceitaBruta() { return receitaBruta; }
    public void addReceita(BigDecimal valor) { this.receitaBruta = this.receitaBruta.add(valor); }


    public BigDecimal getReceitaLiquida() { return receitaBruta.subtract(getImpostos()); }

    public BigDecimal getCmv() { return cmv; }
    public void addCmv(BigDecimal valor) { this.cmv = this.cmv.add(valor); }

    public BigDecimal getLucroBruto() { return getReceitaLiquida().subtract(cmv); }

    public BigDecimal getDespesasFixas() { return despesasFixas; }
    public void addDespesa(BigDecimal valor) { this.despesasFixas = this.despesasFixas.add(valor); }

    public BigDecimal getLucroLiquido() { return getLucroBruto().subtract(despesasFixas); }

    public BigDecimal getMargemLiquida() {
        if (receitaBruta.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return getLucroLiquido().multiply(new BigDecimal("100")).divide(receitaBruta, 2, RoundingMode.HALF_UP);
    }
}