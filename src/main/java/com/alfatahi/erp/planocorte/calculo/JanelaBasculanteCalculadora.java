package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.service.RegraTecnicaService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class JanelaBasculanteCalculadora implements ServicoCalculadora {

    private static final BigDecimal DESLOCAMENTO_RECORTE_SUPERIOR_MM = BigDecimal.valueOf(200);

    private final RegraTecnicaService regraTecnicaService;

    public JanelaBasculanteCalculadora(RegraTecnicaService regraTecnicaService) {
        this.regraTecnicaService = regraTecnicaService;
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.JANELA_BASCULANTE;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        List<RegraTecnica> regras = regraTecnicaService.listarAtivasPorCategoria(getCategoria());
        CalculoUtil.AjusteDetalhado ajuste = CalculoUtil.aplicarRegras(entrada.larguraVaoMm(), entrada.alturaVaoMm(), regras);

        BigDecimal largura = ajuste.larguraFinalMm();
        BigDecimal altura = ajuste.alturaFinalMm();
        BigDecimal centroVertical = altura.divide(BigDecimal.valueOf(2));
        BigDecimal xRecorteSuperior = largura.divide(BigDecimal.valueOf(2)).add(DESLOCAMENTO_RECORTE_SUPERIOR_MM);

        ElementoTecnico recorteDireito = new ElementoTecnico();
        recorteDireito.setTipo(TipoElemento.RECORTE);
        recorteDireito.setLado("Direito");
        recorteDireito.setPosicaoXMm(largura);
        recorteDireito.setPosicaoYMm(centroVertical);
        recorteDireito.setObservacao("Recorte lateral direito, no centro da altura (" + centroVertical.stripTrailingZeros().toPlainString() + "mm) — largura/altura/raio a configurar.");

        ElementoTecnico recorteEsquerdo = new ElementoTecnico();
        recorteEsquerdo.setTipo(TipoElemento.RECORTE);
        recorteEsquerdo.setLado("Esquerdo");
        recorteEsquerdo.setPosicaoXMm(BigDecimal.ZERO);
        recorteEsquerdo.setPosicaoYMm(centroVertical);
        recorteEsquerdo.setObservacao("Recorte lateral esquerdo, no centro da altura (" + centroVertical.stripTrailingZeros().toPlainString() + "mm) — largura/altura/raio a configurar.");

        ElementoTecnico recorteSuperior = new ElementoTecnico();
        recorteSuperior.setTipo(TipoElemento.RECORTE);
        recorteSuperior.setLado("Canto superior");
        recorteSuperior.setPosicaoYMm(BigDecimal.ZERO);
        recorteSuperior.setPosicaoXMm(xRecorteSuperior);
        recorteSuperior.setObservacao("Recorte do canto superior — X = metade da largura (" + largura.divide(BigDecimal.valueOf(2)).stripTrailingZeros().toPlainString()
                + "mm) + 200mm = " + xRecorteSuperior.stripTrailingZeros().toPlainString() + "mm — largura/altura/raio a configurar.");

        String observacao = "Janela Basculante — " + ajuste.resumo()
                + " · 2 recortes laterais (centro da altura) + 1 recorte no canto superior (metade da largura + 200mm) — ver elementos técnicos da peça.";

        FolhaCalculada folha = new FolhaCalculada(TipoFolha.UNICA, largura, altura,
                List.of(), List.of(recorteDireito, recorteEsquerdo, recorteSuperior), observacao);
        return new ResultadoCalculoServico(List.of(folha));
    }
}
