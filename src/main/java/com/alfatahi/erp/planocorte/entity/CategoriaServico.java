package com.alfatahi.erp.planocorte.entity;














public enum CategoriaServico {

    ABRIGO_PIA("Abrigo de Pia"),
    PORTA_CORRER("Porta de Correr"),
    JANELA_PADRAO("Janela Padrão"),
    PORTA_ABRIR("Porta de Abrir"),
    VIDRO_FIXO_PERFIL_U("Vidro Fixo no Perfil U"),
    JANELA_BASCULANTE("Janela Basculante"),
    SACADA("Sacada"),
    ESPELHO("Espelho"),
    BOX_BANHEIRO_PADRAO("Box de Banheiro Padrão");

    private final String descricao;

    CategoriaServico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
