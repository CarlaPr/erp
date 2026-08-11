package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.TipoBorda;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlanoCorteItemForm {

    @NotNull(message = "Selecione o vidro")
    private Long vidroId;

    @NotNull(message = "Informe a largura")
    @DecimalMin(value = "1", message = "Largura deve ser maior que zero")
    private BigDecimal larguraBrutaMm;

    @NotNull(message = "Informe a altura")
    @DecimalMin(value = "1", message = "Altura deve ser maior que zero")
    private BigDecimal alturaBrutaMm;

    @NotNull(message = "Informe a quantidade")
    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantidade;

    @NotNull(message = "Selecione o acabamento")
    private TipoBorda tipoBorda;

    private String observacoes;

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
}
