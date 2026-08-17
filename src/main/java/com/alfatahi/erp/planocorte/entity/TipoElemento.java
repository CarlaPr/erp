package com.alfatahi.erp.planocorte.entity;

public enum TipoElemento {

    FURO("Furo"),
    RASGO("Rasgo"),
    RECORTE("Recorte"),
    CHANFRO("Chanfro"),
    BOLEADO("Boleado");

    private final String descricao;

    TipoElemento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }


    public static TipoElemento[] valoresSelecionaveis() {
        return new TipoElemento[]{FURO, RECORTE, CHANFRO};
    }
}
