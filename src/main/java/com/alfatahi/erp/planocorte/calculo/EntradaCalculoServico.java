package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.TipoVidro;

import java.math.BigDecimal;

public record EntradaCalculoServico(

        BigDecimal larguraVaoMm,
        BigDecimal alturaVaoMm,
        Integer quantidadeFolhas,
        Integer quantidadeFolhasFixas,
        Integer quantidadeFolhasMoveis,
        String ladoRecorte,
        TipoVidro tipoVidro,
        BigDecimal descontoLateralPersonalizadoMm,
        BigDecimal descontoAlturaPersonalizadoMm,
        BigDecimal espessuraBisoteMm,
        Boolean comFechadura,
        Boolean redondo,
        BigDecimal alturaBateFechaMm) {

    public boolean temFechadura() {
        return Boolean.TRUE.equals(comFechadura);
    }

    public boolean isRedondo() {
        return Boolean.TRUE.equals(redondo);
    }
}
