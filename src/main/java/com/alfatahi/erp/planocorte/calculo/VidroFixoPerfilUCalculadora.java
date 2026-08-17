package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.service.RegraTecnicaService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class VidroFixoPerfilUCalculadora implements ServicoCalculadora {

    private final RegraTecnicaService regraTecnicaService;

    public VidroFixoPerfilUCalculadora(RegraTecnicaService regraTecnicaService) {
        this.regraTecnicaService = regraTecnicaService;
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.VIDRO_FIXO_PERFIL_U;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        boolean personalizado = entrada.descontoLateralPersonalizadoMm() != null || entrada.descontoAlturaPersonalizadoMm() != null;

        BigDecimal largura;
        BigDecimal altura;
        String resumo;

        if (personalizado) {
            BigDecimal descontoLateral = entrada.descontoLateralPersonalizadoMm() != null ? entrada.descontoLateralPersonalizadoMm() : BigDecimal.ZERO;
            BigDecimal descontoAltura = entrada.descontoAlturaPersonalizadoMm() != null ? entrada.descontoAlturaPersonalizadoMm() : BigDecimal.ZERO;
            largura = CalculoUtil.descontar(entrada.larguraVaoMm(), descontoLateral);
            altura = CalculoUtil.descontar(entrada.alturaVaoMm(), descontoAltura);
            resumo = "Desconto personalizado deste vão: lateral " + descontoLateral.stripTrailingZeros().toPlainString()
                    + "mm, altura " + descontoAltura.stripTrailingZeros().toPlainString() + "mm.";
        } else {
            List<RegraTecnica> regras = regraTecnicaService.listarAtivasPorCategoria(getCategoria());
            CalculoUtil.AjusteDetalhado ajuste = CalculoUtil.aplicarRegras(entrada.larguraVaoMm(), entrada.alturaVaoMm(), regras);
            largura = ajuste.larguraFinalMm();
            altura = ajuste.alturaFinalMm();
            resumo = ajuste.resumo().isBlank()
                    ? "Nenhuma regra técnica cadastrada ainda em Regras Técnicas (categoria Vidro Fixo) e nenhum desconto personalizado informado — medida final = medida bruta."
                    : ajuste.resumo();
        }

        String observacao = "Vidro Fixo ";

        FolhaCalculada folha = new FolhaCalculada(TipoFolha.FIXA, largura, altura, List.of(), List.of(), observacao);
        return new ResultadoCalculoServico(List.of(folha));
    }
}
