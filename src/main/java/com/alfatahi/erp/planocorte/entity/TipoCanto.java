package com.alfatahi.erp.planocorte.entity;

public enum TipoCanto {
    NORMAL("Nenhum (normal)"), CANTO_MOEDA("Canto moeda"), CANTO_GARRAFA("Canto garrafa");
    private final String descricao;
    TipoCanto(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
