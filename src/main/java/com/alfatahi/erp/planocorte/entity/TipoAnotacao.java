package com.alfatahi.erp.planocorte.entity;


public enum TipoAnotacao {

    TEXTO("Texto"),
    SETA("Seta"),
    LINHA("Linha");

    private final String descricao;

    TipoAnotacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
