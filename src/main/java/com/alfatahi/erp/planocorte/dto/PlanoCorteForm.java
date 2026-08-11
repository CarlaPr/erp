package com.alfatahi.erp.planocorte.dto;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class PlanoCorteForm {

    private Long id;

    @NotNull(message = "Selecione a OS (ordem de serviço)")
    private UUID workOrderId;

    @NotNull(message = "Selecione o serviço")
    private CategoriaServico categoria;

    private String descricao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public void setWorkOrderId(UUID workOrderId) {
        this.workOrderId = workOrderId;
    }

    public CategoriaServico getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaServico categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
