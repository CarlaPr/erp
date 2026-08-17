package com.alfatahi.erp.planocorte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;


@Embeddable
public class Anotacao {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 10)
    private TipoAnotacao tipo;

    @Column(name = "x1_mm", precision = 9, scale = 2)
    private BigDecimal x1Mm;

    @Column(name = "y1_mm", precision = 9, scale = 2)
    private BigDecimal y1Mm;

    @Column(name = "x2_mm", precision = 9, scale = 2)
    private BigDecimal x2Mm;

    @Column(name = "y2_mm", precision = 9, scale = 2)
    private BigDecimal y2Mm;

    @Column(name = "texto", length = 120)
    private String texto;

    public Anotacao() {
    }

    public TipoAnotacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoAnotacao tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getX1Mm() {
        return x1Mm;
    }

    public void setX1Mm(BigDecimal x1Mm) {
        this.x1Mm = x1Mm;
    }

    public BigDecimal getY1Mm() {
        return y1Mm;
    }

    public void setY1Mm(BigDecimal y1Mm) {
        this.y1Mm = y1Mm;
    }

    public BigDecimal getX2Mm() {
        return x2Mm;
    }

    public void setX2Mm(BigDecimal x2Mm) {
        this.x2Mm = x2Mm;
    }

    public BigDecimal getY2Mm() {
        return y2Mm;
    }

    public void setY2Mm(BigDecimal y2Mm) {
        this.y2Mm = y2Mm;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
