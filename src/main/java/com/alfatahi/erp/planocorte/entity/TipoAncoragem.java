package com.alfatahi.erp.planocorte.entity;


public enum TipoAncoragem {

    CENTRO("Centralizado no ponto informado"),
    CANTO("Encostado no canto / borda informada");

    private final String descricao;

    TipoAncoragem(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
