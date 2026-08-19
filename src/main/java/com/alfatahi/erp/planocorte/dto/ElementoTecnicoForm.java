package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.TipoAncoragem;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ElementoTecnicoForm {

    @NotNull(message = "Selecione o tipo")
    private TipoElemento tipo;

    private String nome;

    private TipoAncoragem ancoragem = TipoAncoragem.CENTRO;

    private Long ferragemId;

    private String ferragemNomeSnapshot;

    @NotNull(message = "Selecione a referência horizontal")
    private ReferenciaHorizontal referenciaHorizontal = ReferenciaHorizontal.ESQUERDA;

    @NotNull(message = "Informe a distância horizontal")
    private BigDecimal distanciaHorizontalMm;

    @NotNull(message = "Selecione a referência vertical")
    private ReferenciaVertical referenciaVertical = ReferenciaVertical.SUPERIOR;

    @NotNull(message = "Informe a distância vertical")
    private BigDecimal distanciaVerticalMm;

    private BigDecimal diametroMm;
    private BigDecimal larguraMm;
    private BigDecimal alturaMm;
    private BigDecimal comprimentoMm;
    private BigDecimal profundidadeMm;
    private BigDecimal raioMm;
    private BigDecimal anguloGraus;
    private String orientacao;
    private String formato;
    private String lado;
    private String observacao;

    public TipoElemento getTipo() {
        return tipo;
    }

    public void setTipo(TipoElemento tipo) {
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public TipoAncoragem getAncoragem() {
        return ancoragem;
    }

    public void setAncoragem(TipoAncoragem ancoragem) {
        this.ancoragem = ancoragem;
    }

    public Long getFerragemId() {
        return ferragemId;
    }

    public void setFerragemId(Long ferragemId) {
        this.ferragemId = ferragemId;
    }

    public String getFerragemNomeSnapshot() {
        return ferragemNomeSnapshot;
    }

    public void setFerragemNomeSnapshot(String ferragemNomeSnapshot) {
        this.ferragemNomeSnapshot = ferragemNomeSnapshot;
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

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }
    private Integer referenciaElementoIndice;

    public Integer getReferenciaElementoIndice() {
        return referenciaElementoIndice;
    }

    public void setReferenciaElementoIndice(Integer referenciaElementoIndice) {
        this.referenciaElementoIndice = referenciaElementoIndice;
    }

}
