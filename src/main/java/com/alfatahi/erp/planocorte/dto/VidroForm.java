package com.alfatahi.erp.planocorte.dto;

import java.util.UUID;
import com.alfatahi.erp.planocorte.entity.TipoVidro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VidroForm {

    private Long id;

    @NotBlank(message = "Informe o nome do vidro")
    private String nome;

    private String fabricante;
    private UUID supplierId;

    @NotNull(message = "Informe o tipo do vidro")
    private TipoVidro tipo;

    @NotNull(message = "Informe a espessura")
    private BigDecimal espessura;

    private String cor;
    private String acabamento;

    @NotNull(message = "Informe o valor por m²")
    private BigDecimal valorPorM2;

    private BigDecimal valorMinimo;
    private BigDecimal pesoPorM2;
    private LocalDate dataVigencia;
    private LocalDate dataValidade;
    private String observacoes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getFabricante() {
        return fabricante;
    }

    public void setFabricante(String fabricante) {
        this.fabricante = fabricante;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public TipoVidro getTipo() {
        return tipo;
    }

    public void setTipo(TipoVidro tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getEspessura() {
        return espessura;
    }

    public void setEspessura(BigDecimal espessura) {
        this.espessura = espessura;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getAcabamento() {
        return acabamento;
    }

    public void setAcabamento(String acabamento) {
        this.acabamento = acabamento;
    }

    public BigDecimal getValorPorM2() {
        return valorPorM2;
    }

    public void setValorPorM2(BigDecimal valorPorM2) {
        this.valorPorM2 = valorPorM2;
    }

    public BigDecimal getValorMinimo() {
        return valorMinimo;
    }

    public void setValorMinimo(BigDecimal valorMinimo) {
        this.valorMinimo = valorMinimo;
    }

    public BigDecimal getPesoPorM2() {
        return pesoPorM2;
    }

    public void setPesoPorM2(BigDecimal pesoPorM2) {
        this.pesoPorM2 = pesoPorM2;
    }

    public LocalDate getDataVigencia() {
        return dataVigencia;
    }

    public void setDataVigencia(LocalDate dataVigencia) {
        this.dataVigencia = dataVigencia;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
