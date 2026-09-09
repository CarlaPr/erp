package com.alfatahi.erp.planocorte.entity;

/** Cores disponíveis na especificação técnica de um vão. */
public enum CorVidroVao {
    INCOLOR("Incolor"), FUME("Fumê"), VERDE("Verde"), REFLECTA("Reflecta"), BRONZE("Bronze");

    private final String descricao;
    CorVidroVao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
