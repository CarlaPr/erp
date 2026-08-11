package com.alfatahi.erp.planocorte.entity;

public enum TipoFolha {

    UNICA("Única"),
    FIXA("Fixa"),
    MOVEL("Móvel");

    private final String descricao;

    TipoFolha(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
