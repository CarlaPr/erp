package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPortaCorrerJanelaCalculadora implements ServicoCalculadora {

    private final ParametroServicoService parametroServicoService;

    private final BigDecimal roldanaDistBordaPadraoMm;
    private final BigDecimal roldanaDistTopoPadraoMm;
    private final BigDecimal roldanaQtdPadrao;

    private static final BigDecimal BATE_FECHA_DIST_BORDA_MM = BigDecimal.valueOf(33);
    private static final BigDecimal BATE_FECHA_DIAMETRO_MM = BigDecimal.valueOf(20);
    private static final BigDecimal BATE_FECHA_DIST_ENTRE_FUROS_MM = BigDecimal.valueOf(50);
    private static final BigDecimal BATE_FECHA_DIST_PISO_PADRAO_MM = BigDecimal.valueOf(300);

    protected AbstractPortaCorrerJanelaCalculadora(ParametroServicoService parametroServicoService) {
        this(parametroServicoService, null, null, null);
    }



    protected AbstractPortaCorrerJanelaCalculadora(ParametroServicoService parametroServicoService,
                                                    BigDecimal roldanaDistBordaPadraoMm,
                                                    BigDecimal roldanaDistTopoPadraoMm,
                                                    BigDecimal roldanaQtdPadrao) {
        this.parametroServicoService = parametroServicoService;
        this.roldanaDistBordaPadraoMm = roldanaDistBordaPadraoMm;
        this.roldanaDistTopoPadraoMm = roldanaDistTopoPadraoMm;
        this.roldanaQtdPadrao = roldanaQtdPadrao;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        CategoriaServico cat = getCategoria();

        BigDecimal descontoAlturaFixo = parametroServicoService.valor(cat, "DESCONTO_ALTURA_VIDRO_FIXO_MM", BigDecimal.valueOf(60));
        BigDecimal descontoAlturaMovel = parametroServicoService.valor(cat, "DESCONTO_ALTURA_VIDRO_MOVEL_MM", BigDecimal.valueOf(20));
        BigDecimal transpasse = parametroServicoService.valor(cat, "TRANSPASSE_VIDRO_MOVEL_MM", BigDecimal.valueOf(50));
        BigDecimal roldanaQtd = parametroServicoService.valor(cat, "ROLDANA_QTD_POR_FOLHA_MOVEL", roldanaQtdPadrao);
        BigDecimal distBordaRoldana = parametroServicoService.valor(cat, "ROLDANA_DIST_BORDA_MM", roldanaDistBordaPadraoMm);
        BigDecimal distTopoRoldana = parametroServicoService.valor(cat, "ROLDANA_DIST_TOPO_MM", roldanaDistTopoPadraoMm);

        int quantidadeFixas = quantidadeOuZero(entrada.quantidadeFolhasFixas());
        int quantidadeMoveis = quantidadeOuZero(entrada.quantidadeFolhasMoveis());
        if (quantidadeFixas == 0 && quantidadeMoveis == 0) {
            quantidadeMoveis = 1;
        }
        int totalFolhas = quantidadeFixas + quantidadeMoveis;

        BigDecimal larguraPorFolha = CalculoUtil.dividir(entrada.larguraVaoMm(), totalFolhas);
        BigDecimal alturaFixo = CalculoUtil.descontar(entrada.alturaVaoMm(), descontoAlturaFixo);
        BigDecimal alturaMovel = CalculoUtil.descontar(entrada.alturaVaoMm(), descontoAlturaMovel);

        List<FolhaCalculada> folhas = new ArrayList<>();
        int numero = 1;
        for (int i = 0; i < quantidadeFixas; i++) {
            String observacao = "Folha " + numero + " de " + totalFolhas + " (Vidro fixo) — " + cat.getDescricao() + ":\n "
                    + "vão " + entrada.larguraVaoMm().stripTrailingZeros().toPlainString() + "mm ÷ " + totalFolhas
                    + " = " + larguraPorFolha.stripTrailingZeros().toPlainString() + "mm de largura  · "
                    + "\naltura " + entrada.alturaVaoMm().stripTrailingZeros().toPlainString() + "mm − "
                    + descontoAlturaFixo.stripTrailingZeros().toPlainString() + "mm = " + alturaFixo.stripTrailingZeros().toPlainString() + "mm.";
            folhas.add(new FolhaCalculada(TipoFolha.FIXA, larguraPorFolha, alturaFixo, List.of(), List.of(), observacao));
            numero++;
        }
        for (int i = 0; i < quantidadeMoveis; i++) {
            BigDecimal larguraFabricacao = larguraPorFolha.add(transpasse);
            List<FuracaoCalculada> furos = gerarFuracoesRoldana(larguraFabricacao, distBordaRoldana, distTopoRoldana, roldanaQtd);

            int qtdRoldanas = furos.size();

            String observacao = "Folha " + numero + " de " + totalFolhas + " (Vidro móvel) — " + cat.getDescricao() + "\n "
                    + "vão " + entrada.larguraVaoMm().stripTrailingZeros().toPlainString() + "mm ÷ " + totalFolhas
                    + " = " + larguraPorFolha.stripTrailingZeros().toPlainString() + "mm + transpasse "
                    + transpasse.stripTrailingZeros().toPlainString() + "mm = "
                    + larguraFabricacao.stripTrailingZeros().toPlainString() + "mm de fabricação  \n · "
                    + "altura " + entrada.alturaVaoMm().stripTrailingZeros().toPlainString() + "mm − "
                    + descontoAlturaMovel.stripTrailingZeros().toPlainString() + "mm = " + alturaMovel.stripTrailingZeros().toPlainString() + "mm"
                    + (qtdRoldanas == 0
                            ? " \n"
                            : " \n· " + qtdRoldanas + " furo(s) de roldana a " + distBordaRoldana.stripTrailingZeros().toPlainString()
                                    + "mm da borda lateral (um em cada canto)"
                                    + (distTopoRoldana != null ? " e " + distTopoRoldana.stripTrailingZeros().toPlainString() + "mm do topo." : "."));

            folhas.add(new FolhaCalculada(TipoFolha.MOVEL, larguraFabricacao, alturaMovel, furos, List.of(), observacao));
            numero++;
        }

        return new ResultadoCalculoServico(folhas);
    }

    private int quantidadeOuZero(Integer valor) {
        return valor != null && valor > 0 ? valor : 0;
    }





    private List<FuracaoCalculada> gerarFuracoesBateFecha(BigDecimal larguraFolha, BigDecimal alturaVao,
                                                            BigDecimal alturaFolha, boolean ladoDireito,
                                                            BigDecimal distanciaPisoMm) {
        List<FuracaoCalculada> furos = new ArrayList<>();
        BigDecimal x = ladoDireito
                ? larguraFolha.subtract(BATE_FECHA_DIST_BORDA_MM)
                : BATE_FECHA_DIST_BORDA_MM;

        BigDecimal centroY = alturaVao.subtract(distanciaPisoMm);
        BigDecimal metadeGap = BATE_FECHA_DIST_ENTRE_FUROS_MM.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        if (centroY.subtract(metadeGap).signum() < 0 || centroY.add(metadeGap).compareTo(alturaFolha) > 0) {
            throw new IllegalArgumentException("A altura do bate-fecha posiciona um dos furos fora do vidro.");
        }
        BigDecimal yFuro1 = centroY.subtract(metadeGap);
        BigDecimal yFuro2 = centroY.add(metadeGap);

        furos.add(new FuracaoCalculada(TipoFuracao.BATE_FECHA, x, yFuro1, BATE_FECHA_DIAMETRO_MM,
                "Bate-fecha · centro a " + distanciaPisoMm.stripTrailingZeros().toPlainString() + "mm do chão"));
        furos.add(new FuracaoCalculada(TipoFuracao.BATE_FECHA, x, yFuro2, BATE_FECHA_DIAMETRO_MM, ""));
        return furos;
    }

    private List<FuracaoCalculada> gerarFuracoesRoldana(BigDecimal larguraFolha, BigDecimal distBorda, BigDecimal distTopo, BigDecimal quantidade) {
        List<FuracaoCalculada> furos = new ArrayList<>();
        BigDecimal diametroRoldana = diametroRoldanaMm();
        if (distBorda == null || quantidade == null || quantidade.signum() <= 0) {
            return furos;
        }
        int qtd = quantidade.intValue();
        if (qtd == 1) {
            BigDecimal centro = larguraFolha.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            furos.add(new FuracaoCalculada(TipoFuracao.ROLDANA, centro, distTopo, diametroRoldana, "Roldana"));
            return furos;
        }
        BigDecimal xEsquerda = distBorda;
        BigDecimal xDireita = larguraFolha.subtract(distBorda);
        furos.add(new FuracaoCalculada(TipoFuracao.ROLDANA, xEsquerda, distTopo, diametroRoldana, "Roldana"));
        furos.add(new FuracaoCalculada(TipoFuracao.ROLDANA, xDireita, distTopo, diametroRoldana, "Roldana"));
        if (qtd > 2) {
            BigDecimal vao = xDireita.subtract(xEsquerda);
            int espacos = qtd - 1;
            for (int i = 1; i < espacos; i++) {
                BigDecimal x = xEsquerda.add(vao.multiply(BigDecimal.valueOf(i))
                        .divide(BigDecimal.valueOf(espacos), 2, RoundingMode.HALF_UP));
                furos.add(new FuracaoCalculada(TipoFuracao.ROLDANA, x, distTopo, diametroRoldana, "Roldana"));
            }
        }
        return furos;
    }

    protected BigDecimal diametroRoldanaMm() {
        return BigDecimal.valueOf(30);
    }
}
