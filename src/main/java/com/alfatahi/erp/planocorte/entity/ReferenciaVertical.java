package com.alfatahi.erp.planocorte.entity;

public enum ReferenciaVertical {

    SUPERIOR("Superior"),
    CENTRO("Centro"),
    INFERIOR("Inferior");

    private final String descricao;

    ReferenciaVertical(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
