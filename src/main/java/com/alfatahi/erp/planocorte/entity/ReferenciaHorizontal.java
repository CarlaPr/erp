package com.alfatahi.erp.planocorte.entity;

public enum ReferenciaHorizontal {

    ESQUERDA("Esquerda"),
    CENTRO("Centro"),
    DIREITA("Direita");

    private final String descricao;

    ReferenciaHorizontal(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
