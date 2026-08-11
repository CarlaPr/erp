package com.alfatahi.erp.planocorte.entity;

public enum StatusPlano {

    RASCUNHO("Rascunho"),
    FINALIZADO("Finalizado");

    private final String descricao;

    StatusPlano(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
