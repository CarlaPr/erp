package com.alfatahi.erp.planocorte.entity;

public enum TipoFuracao {

    ROLDANA("Roldana"),
    PUXADOR("Puxador"),
    DOBRADICA("Dobradiça"),
    FECHADURA("Fechadura"),
    RECORTE("Recorte"),
    OUTRO("Outro");

    private final String descricao;

    TipoFuracao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
