package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.OrigemParametro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ParametroServicoForm {

    private Long id;

    @NotNull(message = "Selecione a categoria")
    private CategoriaServico categoria;

    @NotBlank(message = "Informe um código (ex: TRANSPASSE_MM)")
    private String codigo;

    @NotBlank(message = "Informe a descrição")
    private String descricao;

    private BigDecimal valor;
    private String valorTexto;

    @NotNull(message = "Selecione a origem")
    private OrigemParametro origem = OrigemParametro.PARAMETRO_EMPRESA;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CategoriaServico getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaServico categoria) {
        this.categoria = categoria;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getValorTexto() {
        return valorTexto;
    }

    public void setValorTexto(String valorTexto) {
        this.valorTexto = valorTexto;
    }

    public OrigemParametro getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemParametro origem) {
        this.origem = origem;
    }
}
