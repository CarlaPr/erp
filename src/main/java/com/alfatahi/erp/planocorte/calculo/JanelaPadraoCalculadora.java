package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;


@Component
public class JanelaPadraoCalculadora extends AbstractPortaCorrerJanelaCalculadora {

    public JanelaPadraoCalculadora(ParametroServicoService parametroServicoService) {
        super(parametroServicoService, BigDecimal.valueOf(85), BigDecimal.valueOf(70), BigDecimal.valueOf(2));
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.JANELA_PADRAO;
    }

    @Override
    protected BigDecimal diametroRoldanaMm() {
        return BigDecimal.valueOf(15);
    }
}
