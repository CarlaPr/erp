package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.TipoFuracao;

import java.math.BigDecimal;

public class FuracaoForm {
    private TipoFuracao tipo;
    private BigDecimal posicaoXMm;
    private BigDecimal posicaoYMm;
    private BigDecimal diametroMm;
    private String descricao;

    public TipoFuracao getTipo() { return tipo; }
    public void setTipo(TipoFuracao value) { this.tipo = value; }
    public BigDecimal getPosicaoXMm() { return posicaoXMm; }
    public void setPosicaoXMm(BigDecimal value) { this.posicaoXMm = value; }
    public BigDecimal getPosicaoYMm() { return posicaoYMm; }
    public void setPosicaoYMm(BigDecimal value) { this.posicaoYMm = value; }
    public BigDecimal getDiametroMm() { return diametroMm; }
    public void setDiametroMm(BigDecimal value) { this.diametroMm = value; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String value) { this.descricao = value; }
}
