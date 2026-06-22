package com.alfatahi.erp.dto;

import java.math.BigDecimal;

public class DreReportDto {
    private BigDecimal receitasBrutas = BigDecimal.ZERO;
    private BigDecimal custosVariaveis = BigDecimal.ZERO; // Matéria-prima / Insumos
    private BigDecimal margemContribuicao = BigDecimal.ZERO;
    private BigDecimal custosFixos = BigDecimal.ZERO; // Aluguel, Provisões, etc.
    private BigDecimal resultadoLiquido = BigDecimal.ZERO; // Lucro ou Prejuízo

    // Getters e Setters
    public BigDecimal getReceitasBrutas() { return receitasBrutas; }
    public void setReceitasBrutas(BigDecimal receitasBrutas) { this.receitasBrutas = receitasBrutas; }
    public BigDecimal getCustosVariaveis() { return custosVariaveis; }
    public void setCustosVariaveis(BigDecimal custosVariaveis) { this.custosVariaveis = custosVariaveis; }
    public BigDecimal getMargemContribuicao() { return margemContribuicao; }
    public void setMargemContribuicao(BigDecimal margemContribuicao) { this.margemContribuicao = margemContribuicao; }
    public BigDecimal getCustosFixos() { return custosFixos; }
    public void setCustosFixos(BigDecimal custosFixos) { this.custosFixos = custosFixos; }
    public BigDecimal getResultadoLiquido() { return resultadoLiquido; }
    public void setResultadoLiquido(BigDecimal resultadoLiquido) { this.resultadoLiquido = resultadoLiquido; }
}