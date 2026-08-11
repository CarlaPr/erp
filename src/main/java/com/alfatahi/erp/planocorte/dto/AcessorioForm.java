package com.alfatahi.erp.planocorte.dto;

import java.util.UUID;
import com.alfatahi.erp.planocorte.entity.TipoAcessorio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AcessorioForm {

    private Long id;

    @NotBlank(message = "Informe o nome")
    private String nome;

    @NotNull(message = "Selecione o tipo")
    private TipoAcessorio tipo;

    private String unidade = "un";
    private UUID supplierId;

    @NotNull(message = "Informe o valor")
    private BigDecimal valor;

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

    public TipoAcessorio getTipo() {
        return tipo;
    }

    public void setTipo(TipoAcessorio tipo) {
        this.tipo = tipo;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
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
}
