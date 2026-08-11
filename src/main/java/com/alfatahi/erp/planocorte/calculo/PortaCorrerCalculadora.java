package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PortaCorrerCalculadora extends AbstractPortaCorrerJanelaCalculadora {

    public PortaCorrerCalculadora(ParametroServicoService parametroServicoService) {
        super(parametroServicoService, BigDecimal.valueOf(125), BigDecimal.valueOf(70), BigDecimal.valueOf(2));
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.PORTA_CORRER;
    }
}
