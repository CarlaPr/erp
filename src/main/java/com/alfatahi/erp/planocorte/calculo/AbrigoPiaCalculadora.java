package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.service.RegraTecnicaService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AbrigoPiaCalculadora implements ServicoCalculadora {

    private final RegraTecnicaService regraTecnicaService;

    public AbrigoPiaCalculadora(RegraTecnicaService regraTecnicaService) {
        this.regraTecnicaService = regraTecnicaService;
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.ABRIGO_PIA;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        List<RegraTecnica> regras = regraTecnicaService.listarAtivasPorCategoria(getCategoria());
        CalculoUtil.AjusteDetalhado ajuste = CalculoUtil.aplicarRegras(entrada.larguraVaoMm(), entrada.alturaVaoMm(), regras);

        String observacao = "Abrigo de Pia —  "
                + (ajuste.resumo().isBlank()
                        ? " = medida bruta."
                        : ajuste.resumo());

        FolhaCalculada folha = new FolhaCalculada(TipoFolha.UNICA, ajuste.larguraFinalMm(), ajuste.alturaFinalMm(),
                List.of(), List.of(), observacao);
        return new ResultadoCalculoServico(List.of(folha));
    }
}
