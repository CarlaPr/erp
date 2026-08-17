package com.alfatahi.erp.planocorte.entity;

public enum TipoBorda {

    LISO("Liso"),
    LAPIDADO("Lapidado"),
    BISOTADO("Bisotado"),
    JATEADO("Jateado"),
    ACIDATO("Acidato"),
    SERIGRAFADO("Serigrafado"),
    PINTADO("Pintado"),
    ADESIVADO("Adesivado"),
    CANTO_MOEDA("Canto Moeda"),
    CANTO_GARRAFA("Canto Garrafa");

    private final String descricao;

    TipoBorda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
