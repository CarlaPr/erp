package com.alfatahi.erp.planocorte.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PerfilAluminioForm {

    private Long id;

    @NotBlank(message = "Informe a linha")
    private String linha;

    @NotBlank(message = "Informe o perfil")
    private String perfil;

    private String cor;
    private String fabricante;
    private UUID supplierId;

    @NotNull(message = "Informe o valor por metro")
    private BigDecimal valorPorMetro;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLinha() {
        return linha;
    }

    public void setLinha(String linha) {
        this.linha = linha;
    }

    public String getPerfil() {
        return perfil;
    }

    public void setPerfil(String perfil) {
        this.perfil = perfil;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
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

    public BigDecimal getValorPorMetro() {
        return valorPorMetro;
    }

    public void setValorPorMetro(BigDecimal valorPorMetro) {
        this.valorPorMetro = valorPorMetro;
    }
}
