package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.TipoBorda;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlanoCorteVaoForm {

    @NotNull(message = "Selecione o vidro")
    private Long vidroId;

    private BigDecimal larguraVaoMm;

    private BigDecimal alturaVaoMm;








    private boolean dimensoesPersonalizadas = false;

    private BigDecimal alturaBrutaEsquerdaMm;
    private BigDecimal alturaBrutaDireitaMm;
    private BigDecimal larguraBrutaSuperiorMm;
    private BigDecimal larguraBrutaInferiorMm;

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

    private boolean espelhoRedondo = false;

    private BigDecimal alturaBateFechaMm;

    private String observacoes;


    private boolean cantoSuperiorEsquerdo = false;
    private boolean cantoSuperiorDireito = false;
    private boolean cantoInferiorEsquerdo = false;
    private boolean cantoInferiorDireito = false;

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

    public boolean isDimensoesPersonalizadas() {
        return dimensoesPersonalizadas;
    }

    public void setDimensoesPersonalizadas(boolean dimensoesPersonalizadas) {
        this.dimensoesPersonalizadas = dimensoesPersonalizadas;
    }

    public BigDecimal getAlturaBrutaEsquerdaMm() {
        return alturaBrutaEsquerdaMm;
    }

    public void setAlturaBrutaEsquerdaMm(BigDecimal alturaBrutaEsquerdaMm) {
        this.alturaBrutaEsquerdaMm = alturaBrutaEsquerdaMm;
    }

    public BigDecimal getAlturaBrutaDireitaMm() {
        return alturaBrutaDireitaMm;
    }

    public void setAlturaBrutaDireitaMm(BigDecimal alturaBrutaDireitaMm) {
        this.alturaBrutaDireitaMm = alturaBrutaDireitaMm;
    }

    public BigDecimal getLarguraBrutaSuperiorMm() {
        return larguraBrutaSuperiorMm;
    }

    public void setLarguraBrutaSuperiorMm(BigDecimal larguraBrutaSuperiorMm) {
        this.larguraBrutaSuperiorMm = larguraBrutaSuperiorMm;
    }

    public BigDecimal getLarguraBrutaInferiorMm() {
        return larguraBrutaInferiorMm;
    }

    public void setLarguraBrutaInferiorMm(BigDecimal larguraBrutaInferiorMm) {
        this.larguraBrutaInferiorMm = larguraBrutaInferiorMm;
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

    public boolean isEspelhoRedondo() {
        return espelhoRedondo;
    }

    public void setEspelhoRedondo(boolean espelhoRedondo) {
        this.espelhoRedondo = espelhoRedondo;
    }

    public BigDecimal getAlturaBateFechaMm() {
        return alturaBateFechaMm;
    }

    public void setAlturaBateFechaMm(BigDecimal alturaBateFechaMm) {
        this.alturaBateFechaMm = alturaBateFechaMm;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public boolean isCantoSuperiorEsquerdo() {
        return cantoSuperiorEsquerdo;
    }

    public void setCantoSuperiorEsquerdo(boolean cantoSuperiorEsquerdo) {
        this.cantoSuperiorEsquerdo = cantoSuperiorEsquerdo;
    }

    public boolean isCantoSuperiorDireito() {
        return cantoSuperiorDireito;
    }

    public void setCantoSuperiorDireito(boolean cantoSuperiorDireito) {
        this.cantoSuperiorDireito = cantoSuperiorDireito;
    }

    public boolean isCantoInferiorEsquerdo() {
        return cantoInferiorEsquerdo;
    }

    public void setCantoInferiorEsquerdo(boolean cantoInferiorEsquerdo) {
        this.cantoInferiorEsquerdo = cantoInferiorEsquerdo;
    }

    public boolean isCantoInferiorDireito() {
        return cantoInferiorDireito;
    }

    public void setCantoInferiorDireito(boolean cantoInferiorDireito) {
        this.cantoInferiorDireito = cantoInferiorDireito;
    }
}
