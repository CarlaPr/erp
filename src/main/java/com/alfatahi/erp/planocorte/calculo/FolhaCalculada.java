package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.TipoFolha;

import java.math.BigDecimal;
import java.util.List;

public record FolhaCalculada(TipoFolha tipoFolha, BigDecimal larguraMm, BigDecimal alturaMm,
                              List<FuracaoCalculada> furacoes,
                              List<ElementoTecnico> elementos,
                              String observacao,
                              BigDecimal espessuraBisoteMm) {

    public FolhaCalculada(TipoFolha tipoFolha, BigDecimal larguraMm, BigDecimal alturaMm,
                           List<FuracaoCalculada> furacoes, List<ElementoTecnico> elementos, String observacao) {
        this(tipoFolha, larguraMm, alturaMm, furacoes, elementos, observacao, null);
    }
}
