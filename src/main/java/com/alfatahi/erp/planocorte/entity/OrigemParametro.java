package com.alfatahi.erp.planocorte.entity;



public enum OrigemParametro {

    NORMA_TECNICA("Norma técnica (verificada)"),
    RECOMENDACAO_FABRICANTE("Recomendação de fabricante"),
    BOA_PRATICA("Boa prática de fabricação"),
    PARAMETRO_EMPRESA("Parâmetro definido pela empresa");

    private final String descricao;

    OrigemParametro(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
