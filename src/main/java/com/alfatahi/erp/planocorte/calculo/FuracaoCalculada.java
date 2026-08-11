package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.TipoFuracao;

import java.math.BigDecimal;

public record FuracaoCalculada(TipoFuracao tipo, BigDecimal posicaoXMm, BigDecimal posicaoYMm,
                                BigDecimal diametroMm, String descricao) {
}
