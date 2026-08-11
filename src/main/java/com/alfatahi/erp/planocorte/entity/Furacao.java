package com.alfatahi.erp.planocorte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;



@Embeddable
public class Furacao {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private TipoFuracao tipo;

    @Column(name = "posicao_x_mm", precision = 8, scale = 2)
    private BigDecimal posicaoXMm;

    @Column(name = "posicao_y_mm", precision = 8, scale = 2)
    private BigDecimal posicaoYMm;


    @Column(name = "diametro_mm", precision = 8, scale = 2)
    private BigDecimal diametroMm;

    @Column(name = "descricao", length = 100)
    private String descricao;

    public Furacao() {
    }

    public Furacao(TipoFuracao tipo, BigDecimal posicaoXMm, BigDecimal posicaoYMm, BigDecimal diametroMm, String descricao) {
        this.tipo = tipo;
        this.posicaoXMm = posicaoXMm;
        this.posicaoYMm = posicaoYMm;
        this.diametroMm = diametroMm;
        this.descricao = descricao;
    }

    public TipoFuracao getTipo() {
        return tipo;
    }

    public void setTipo(TipoFuracao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPosicaoXMm() {
        return posicaoXMm;
    }

    public void setPosicaoXMm(BigDecimal posicaoXMm) {
        this.posicaoXMm = posicaoXMm;
    }

    public BigDecimal getPosicaoYMm() {
        return posicaoYMm;
    }

    public void setPosicaoYMm(BigDecimal posicaoYMm) {
        this.posicaoYMm = posicaoYMm;
    }

    public BigDecimal getDiametroMm() {
        return diametroMm;
    }

    public void setDiametroMm(BigDecimal diametroMm) {
        this.diametroMm = diametroMm;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
