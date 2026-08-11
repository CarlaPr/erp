package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.DimensaoAjuste;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RegraTecnicaForm {

    private Long id;

    @NotNull(message = "Selecione a categoria")
    private CategoriaServico categoria;

    @NotBlank(message = "Informe um código (ex: FOLGA_LATERAL)")
    private String codigo;

    @NotBlank(message = "Informe a descrição")
    private String descricao;

    @NotNull(message = "Selecione a dimensão")
    private DimensaoAjuste dimensao;

    @NotNull(message = "Informe o valor em mm")
    private BigDecimal valorMm;

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

    public DimensaoAjuste getDimensao() {
        return dimensao;
    }

    public void setDimensao(DimensaoAjuste dimensao) {
        this.dimensao = dimensao;
    }

    public BigDecimal getValorMm() {
        return valorMm;
    }

    public void setValorMm(BigDecimal valorMm) {
        this.valorMm = valorMm;
    }
}
