package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.TipoBorda;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlanoCorteVaoForm {

    @NotNull(message = "Selecione o vidro")
    private Long vidroId;

    @NotNull(message = "Informe a largura do vão")
    @DecimalMin(value = "1", message = "Largura deve ser maior que zero")
    private BigDecimal larguraVaoMm;

    @NotNull(message = "Informe a altura do vão")
    @DecimalMin(value = "1", message = "Altura deve ser maior que zero")
    private BigDecimal alturaVaoMm;

    @Min(value = 1, message = "Mínimo 1 folha")
    private Integer quantidadeFolhas;

    @NotNull(message = "Informe a quantidade de vãos iguais")
    @Min(value = 1, message = "Mínimo 1 vão")
    private Integer quantidadeVaos = 1;

    @NotNull(message = "Selecione o acabamento")
    private TipoBorda tipoBorda;

    private Boolean comFechadura = Boolean.FALSE;

    private Integer quantidadeFolhasFixas;
    private Integer quantidadeFolhasMoveis;

    private String ladoRecorte;

    private BigDecimal descontoLateralPersonalizadoMm;
    private BigDecimal descontoAlturaPersonalizadoMm;


    private BigDecimal espessuraBisoteMm;

    private String observacoes;

    public Long getVidroId() {
        return vidroId;
    }

    public void setVidroId(Long vidroId) {
        this.vidroId = vidroId;
    }

    public BigDecimal getLarguraVaoMm() {
        return larguraVaoMm;
    }

    public void setLarguraVaoMm(BigDecimal larguraVaoMm) {
        this.larguraVaoMm = larguraVaoMm;
    }

    public BigDecimal getAlturaVaoMm() {
        return alturaVaoMm;
    }

    public void setAlturaVaoMm(BigDecimal alturaVaoMm) {
        this.alturaVaoMm = alturaVaoMm;
    }

    public Integer getQuantidadeFolhas() {
        return quantidadeFolhas;
    }

    public void setQuantidadeFolhas(Integer quantidadeFolhas) {
        this.quantidadeFolhas = quantidadeFolhas;
    }

    public Integer getQuantidadeVaos() {
        return quantidadeVaos;
    }

    public void setQuantidadeVaos(Integer quantidadeVaos) {
        this.quantidadeVaos = quantidadeVaos;
    }

    public TipoBorda getTipoBorda() {
        return tipoBorda;
    }

    public void setTipoBorda(TipoBorda tipoBorda) {
        this.tipoBorda = tipoBorda;
    }

    public Boolean getComFechadura() {
        return comFechadura;
    }

    public void setComFechadura(Boolean comFechadura) {
        this.comFechadura = comFechadura;
    }

    public Integer getQuantidadeFolhasFixas() {
        return quantidadeFolhasFixas;
    }

    public void setQuantidadeFolhasFixas(Integer quantidadeFolhasFixas) {
        this.quantidadeFolhasFixas = quantidadeFolhasFixas;
    }

    public Integer getQuantidadeFolhasMoveis() {
        return quantidadeFolhasMoveis;
    }

    public void setQuantidadeFolhasMoveis(Integer quantidadeFolhasMoveis) {
        this.quantidadeFolhasMoveis = quantidadeFolhasMoveis;
    }

    public String getLadoRecorte() {
        return ladoRecorte;
    }

    public void setLadoRecorte(String ladoRecorte) {
        this.ladoRecorte = ladoRecorte;
    }

    public BigDecimal getDescontoLateralPersonalizadoMm() {
        return descontoLateralPersonalizadoMm;
    }

    public void setDescontoLateralPersonalizadoMm(BigDecimal descontoLateralPersonalizadoMm) {
        this.descontoLateralPersonalizadoMm = descontoLateralPersonalizadoMm;
    }

    public BigDecimal getDescontoAlturaPersonalizadoMm() {
        return descontoAlturaPersonalizadoMm;
    }

    public void setDescontoAlturaPersonalizadoMm(BigDecimal descontoAlturaPersonalizadoMm) {
        this.descontoAlturaPersonalizadoMm = descontoAlturaPersonalizadoMm;
    }

    public BigDecimal getEspessuraBisoteMm() {
        return espessuraBisoteMm;
    }

    public void setEspessuraBisoteMm(BigDecimal espessuraBisoteMm) {
        this.espessuraBisoteMm = espessuraBisoteMm;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
