package com.alfatahi.erp.planocorte.entity;

public enum OrigemHistorico {

    MANUAL("Edição manual"),
    IMPORTACAO("Importação de planilha");

    private final String descricao;

    OrigemHistorico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
