package com.alfatahi.erp.planocorte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;









@Embeddable
public class ElementoTecnico {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20)
    private TipoElemento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_horizontal", length = 20)
    private ReferenciaHorizontal referenciaHorizontal;

    @Column(name = "distancia_horizontal_mm", precision = 8, scale = 2)
    private BigDecimal distanciaHorizontalMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "referencia_vertical", length = 20)
    private ReferenciaVertical referenciaVertical;

    @Column(name = "distancia_vertical_mm", precision = 8, scale = 2)
    private BigDecimal distanciaVerticalMm;




    @Column(name = "posicao_x_mm", precision = 8, scale = 2)
    private BigDecimal posicaoXMm;

    @Column(name = "posicao_y_mm", precision = 8, scale = 2)
    private BigDecimal posicaoYMm;

    @Column(name = "diametro_mm", precision = 8, scale = 2)
    private BigDecimal diametroMm; // furo

    @Column(name = "largura_mm", precision = 8, scale = 2)
    private BigDecimal larguraMm; // rasgo, recorte

    @Column(name = "altura_mm", precision = 8, scale = 2)
    private BigDecimal alturaMm; // rasgo, recorte

    @Column(name = "comprimento_mm", precision = 8, scale = 2)
    private BigDecimal comprimentoMm; // rasgo, chanfro

    @Column(name = "profundidade_mm", precision = 8, scale = 2)
    private BigDecimal profundidadeMm; // recorte

    @Column(name = "raio_mm", precision = 8, scale = 2)
    private BigDecimal raioMm; // rasgo, recorte, boleado

    @Column(name = "angulo_graus", precision = 6, scale = 2)
    private BigDecimal anguloGraus; // chanfro

    @Column(name = "orientacao", length = 30)
    private String orientacao; // rasgo (ex: "horizontal", "vertical")

    @Column(name = "formato", length = 30)
    private String formato; // recorte (ex: "retangular", "circular", "oval")

    @Column(name = "lado", length = 20)
    private String lado; // boleado, chanfro (ex: "superior esquerdo")



    @Column(name = "ferragem_nome_snapshot", length = 150)
    private String ferragemNomeSnapshot;

    @Column(name = "observacao", length = 200)
    private String observacao;

    public ElementoTecnico() {
    }

    public TipoElemento getTipo() {
        return tipo;
    }

    public void setTipo(TipoElemento tipo) {
        this.tipo = tipo;
    }

    public ReferenciaHorizontal getReferenciaHorizontal() {
        return referenciaHorizontal;
    }

    public void setReferenciaHorizontal(ReferenciaHorizontal referenciaHorizontal) {
        this.referenciaHorizontal = referenciaHorizontal;
    }

    public BigDecimal getDistanciaHorizontalMm() {
        return distanciaHorizontalMm;
    }

    public void setDistanciaHorizontalMm(BigDecimal distanciaHorizontalMm) {
        this.distanciaHorizontalMm = distanciaHorizontalMm;
    }

    public ReferenciaVertical getReferenciaVertical() {
        return referenciaVertical;
    }

    public void setReferenciaVertical(ReferenciaVertical referenciaVertical) {
        this.referenciaVertical = referenciaVertical;
    }

    public BigDecimal getDistanciaVerticalMm() {
        return distanciaVerticalMm;
    }

    public void setDistanciaVerticalMm(BigDecimal distanciaVerticalMm) {
        this.distanciaVerticalMm = distanciaVerticalMm;
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

    public BigDecimal getLarguraMm() {
        return larguraMm;
    }

    public void setLarguraMm(BigDecimal larguraMm) {
        this.larguraMm = larguraMm;
    }

    public BigDecimal getAlturaMm() {
        return alturaMm;
    }

    public void setAlturaMm(BigDecimal alturaMm) {
        this.alturaMm = alturaMm;
    }

    public BigDecimal getComprimentoMm() {
        return comprimentoMm;
    }

    public void setComprimentoMm(BigDecimal comprimentoMm) {
        this.comprimentoMm = comprimentoMm;
    }

    public BigDecimal getProfundidadeMm() {
        return profundidadeMm;
    }

    public void setProfundidadeMm(BigDecimal profundidadeMm) {
        this.profundidadeMm = profundidadeMm;
    }

    public BigDecimal getRaioMm() {
        return raioMm;
    }

    public void setRaioMm(BigDecimal raioMm) {
        this.raioMm = raioMm;
    }

    public BigDecimal getAnguloGraus() {
        return anguloGraus;
    }

    public void setAnguloGraus(BigDecimal anguloGraus) {
        this.anguloGraus = anguloGraus;
    }

    public String getOrientacao() {
        return orientacao;
    }

    public void setOrientacao(String orientacao) {
        this.orientacao = orientacao;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getLado() {
        return lado;
    }

    public void setLado(String lado) {
        this.lado = lado;
    }

    public String getFerragemNomeSnapshot() {
        return ferragemNomeSnapshot;
    }

    public void setFerragemNomeSnapshot(String ferragemNomeSnapshot) {
        this.ferragemNomeSnapshot = ferragemNomeSnapshot;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
}
