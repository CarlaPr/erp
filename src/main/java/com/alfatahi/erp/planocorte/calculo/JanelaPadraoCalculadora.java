package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;
import org.springframework.stereotype.Component;


@Component
public class JanelaPadraoCalculadora extends AbstractPortaCorrerJanelaCalculadora {

    public JanelaPadraoCalculadora(ParametroServicoService parametroServicoService) {
        super(parametroServicoService);
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.JANELA_PADRAO;
    }
}
