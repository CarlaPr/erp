package com.alfatahi.erp.planocorte.entity;

public enum TipoAcessorio {

    PARAFUSO("Parafuso"),
    BOTAO("Botão"),
    BUCHA("Bucha"),
    CALCO("Calço"),
    FITA("Fita"),
    ROLDANA("Roldana"),
    KIT("Kit"),
    CANTONEIRA("Cantoneira"),
    OUTRO("Outro");

    private final String descricao;

    TipoAcessorio(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
