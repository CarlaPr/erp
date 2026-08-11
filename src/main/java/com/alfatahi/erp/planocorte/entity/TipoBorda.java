package com.alfatahi.erp.planocorte.entity;

public enum TipoBorda {

    LISO("Liso"),
    LAPIDADO("Lapidado"),
    BISOTADO("Bisotado");

    private final String descricao;

    TipoBorda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
