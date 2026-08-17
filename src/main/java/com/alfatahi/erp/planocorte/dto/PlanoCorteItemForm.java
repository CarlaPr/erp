package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.TipoBorda;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlanoCorteItemForm {

    @NotNull(message = "Selecione o vidro")
    private Long vidroId;

    private BigDecimal larguraBrutaMm;

    private BigDecimal alturaBrutaMm;


    private boolean dimensoesPersonalizadas = false;

    private BigDecimal alturaBrutaEsquerdaMm;
    private BigDecimal alturaBrutaDireitaMm;
    private BigDecimal larguraBrutaSuperiorMm;
    private BigDecimal larguraBrutaInferiorMm;

    @NotNull(message = "Informe a quantidade")
    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantidade;

    @NotNull(message = "Selecione o acabamento")
    private TipoBorda tipoBorda;

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

    public BigDecimal getLarguraBrutaMm() {
        return larguraBrutaMm;
    }

    public void setLarguraBrutaMm(BigDecimal larguraBrutaMm) {
        this.larguraBrutaMm = larguraBrutaMm;
    }

    public BigDecimal getAlturaBrutaMm() {
        return alturaBrutaMm;
    }

    public void setAlturaBrutaMm(BigDecimal alturaBrutaMm) {
        this.alturaBrutaMm = alturaBrutaMm;
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

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public TipoBorda getTipoBorda() {
        return tipoBorda;
    }

    public void setTipoBorda(TipoBorda tipoBorda) {
        this.tipoBorda = tipoBorda;
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
