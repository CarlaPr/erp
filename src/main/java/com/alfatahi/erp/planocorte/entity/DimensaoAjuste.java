package com.alfatahi.erp.planocorte.entity;

public enum DimensaoAjuste {

    LARGURA("Largura"),
    ALTURA("Altura"),
    LARGURA_E_ALTURA("Largura e altura");

    private final String descricao;

    DimensaoAjuste(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
