package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;

public interface ServicoCalculadora {

    CategoriaServico getCategoria();

    ResultadoCalculoServico calcular(EntradaCalculoServico entrada);
}
