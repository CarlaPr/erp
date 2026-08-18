package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.entity.TipoAncoragem;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.service.RegraTecnicaService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class JanelaBasculanteCalculadora implements ServicoCalculadora {

    private static final BigDecimal DISTANCIA_RECORTE_SUPERIOR_CANTO_DIREITO_MM = BigDecimal.valueOf(20);

    private static final BigDecimal FATOR_ALTURA_RECORTE_LATERAL = new BigDecimal("0.30");

    private static final BigDecimal RECORTE_FERRAGEM_MM = BigDecimal.valueOf(40);
    private static final String ROTULO_CROQUI_RECORTE = "";

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
        BigDecimal yRecorteLateral = altura.multiply(FATOR_ALTURA_RECORTE_LATERAL);
        BigDecimal xRecorteSuperior = largura.subtract(DISTANCIA_RECORTE_SUPERIOR_CANTO_DIREITO_MM);

        ElementoTecnico recorteDireito = new ElementoTecnico();
        recorteDireito.setTipo(TipoElemento.RECORTE);
        recorteDireito.setLado("Direito");
        recorteDireito.setPosicaoXMm(largura);
        recorteDireito.setPosicaoYMm(yRecorteLateral);
        recorteDireito.setLarguraMm(RECORTE_FERRAGEM_MM);
        recorteDireito.setAlturaMm(RECORTE_FERRAGEM_MM);


        recorteDireito.setAncoragem(TipoAncoragem.CANTO);
        recorteDireito.setReferenciaHorizontal(ReferenciaHorizontal.DIREITA);
        recorteDireito.setReferenciaVertical(ReferenciaVertical.CENTRO);
//        recorteDireito.setObservacao("Recorte lateral direito, a " + yRecorteLateral.stripTrailingZeros().toPlainString()
//                + "mm do topo (mais acima que o centro da altura), encostado na borda.");
        recorteDireito.setRotuloCroqui(ROTULO_CROQUI_RECORTE);

        ElementoTecnico recorteEsquerdo = new ElementoTecnico();
        recorteEsquerdo.setTipo(TipoElemento.RECORTE);
        recorteEsquerdo.setLado("Esquerdo");
        recorteEsquerdo.setPosicaoXMm(BigDecimal.ZERO);
        recorteEsquerdo.setPosicaoYMm(yRecorteLateral);
        recorteEsquerdo.setLarguraMm(RECORTE_FERRAGEM_MM);
        recorteEsquerdo.setAlturaMm(RECORTE_FERRAGEM_MM);

        recorteEsquerdo.setAncoragem(TipoAncoragem.CANTO);
        recorteEsquerdo.setReferenciaHorizontal(ReferenciaHorizontal.ESQUERDA);
        recorteEsquerdo.setReferenciaVertical(ReferenciaVertical.CENTRO);
//        recorteEsquerdo.setObservacao("Recorte lateral esquerdo, a " + yRecorteLateral.stripTrailingZeros().toPlainString()
//                + "mm do topo (mais acima que o centro da altura), encostado na borda.");
        recorteEsquerdo.setRotuloCroqui(ROTULO_CROQUI_RECORTE);

        ElementoTecnico recorteSuperior = new ElementoTecnico();
        recorteSuperior.setTipo(TipoElemento.RECORTE);
        recorteSuperior.setLado("Canto superior direito");
        recorteSuperior.setPosicaoYMm(BigDecimal.ZERO);
        recorteSuperior.setPosicaoXMm(xRecorteSuperior);
        recorteSuperior.setLarguraMm(RECORTE_FERRAGEM_MM);
        recorteSuperior.setAlturaMm(RECORTE_FERRAGEM_MM);

        recorteSuperior.setAncoragem(TipoAncoragem.CANTO);
        recorteSuperior.setReferenciaHorizontal(ReferenciaHorizontal.DIREITA);
        recorteSuperior.setReferenciaVertical(ReferenciaVertical.SUPERIOR);
//        recorteSuperior.setObservacao("Recorte do canto superior direito — a "
//                + DISTANCIA_RECORTE_SUPERIOR_CANTO_DIREITO_MM.stripTrailingZeros().toPlainString()
//                + "mm do canto direito, encostado no topo.");
        recorteSuperior.setRotuloCroqui(ROTULO_CROQUI_RECORTE);

        String observacao = "Janela Basculante — " + ajuste.resumo()
                + " · 2 recortes laterais + 1 recorte no canto superior direito";

        FolhaCalculada folha = new FolhaCalculada(TipoFolha.UNICA, largura, altura,
                List.of(), List.of(recorteDireito, recorteEsquerdo, recorteSuperior), observacao);
        return new ResultadoCalculoServico(List.of(folha));
    }
}
