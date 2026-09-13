package com.alfatahi.erp.planocorte.entity;

public enum TipoVidro {
    COMUM,
    TEMPERADO,
    LAMINADO,
    ARAMADO,
    INSULADO,
    ESPELHO,
    ESPELHO_CEBRACE,
    ACRILICO;

    public String getDescricao() {
        if (this == ESPELHO_CEBRACE) return "Espelho Cebrace";
        return name().substring(0, 1) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
