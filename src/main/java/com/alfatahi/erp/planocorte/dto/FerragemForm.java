package com.alfatahi.erp.planocorte.dto;

import java.util.UUID;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class FerragemForm {

    private Long id;

    @NotBlank(message = "Informe a categoria (ex: Dobradiça, Puxador)")
    private String categoria;

    @NotBlank(message = "Informe o produto")
    private String produto;

    private String codigo;
    private UUID supplierId;

    @NotNull(message = "Informe o valor")
    private BigDecimal valor;

    private String unidade = "un";


    private TipoElemento tipoElementoPadrao;
    private BigDecimal diametroFuracaoMm;
    private Integer quantidadeFuros;
    private BigDecimal distanciaBordaMm;
    private BigDecimal distanciaTopoMm;
    private BigDecimal larguraMm;
    private BigDecimal alturaMm;
    private BigDecimal comprimentoMm;
    private BigDecimal profundidadeMm;
    private BigDecimal raioMm;
    private BigDecimal anguloGraus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getProduto() {
        return produto;
    }

    public void setProduto(String produto) {
        this.produto = produto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public TipoElemento getTipoElementoPadrao() {
        return tipoElementoPadrao;
    }

    public void setTipoElementoPadrao(TipoElemento tipoElementoPadrao) {
        this.tipoElementoPadrao = tipoElementoPadrao;
    }

    public BigDecimal getDiametroFuracaoMm() {
        return diametroFuracaoMm;
    }

    public void setDiametroFuracaoMm(BigDecimal diametroFuracaoMm) {
        this.diametroFuracaoMm = diametroFuracaoMm;
    }

    public Integer getQuantidadeFuros() {
        return quantidadeFuros;
    }

    public void setQuantidadeFuros(Integer quantidadeFuros) {
        this.quantidadeFuros = quantidadeFuros;
    }

    public BigDecimal getDistanciaBordaMm() {
        return distanciaBordaMm;
    }

    public void setDistanciaBordaMm(BigDecimal distanciaBordaMm) {
        this.distanciaBordaMm = distanciaBordaMm;
    }

    public BigDecimal getDistanciaTopoMm() {
        return distanciaTopoMm;
    }

    public void setDistanciaTopoMm(BigDecimal distanciaTopoMm) {
        this.distanciaTopoMm = distanciaTopoMm;
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
}
