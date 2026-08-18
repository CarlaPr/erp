package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class BoxBanheiroCalculadora implements ServicoCalculadora {

    private final ParametroServicoService parametroServicoService;

    public BoxBanheiroCalculadora(ParametroServicoService parametroServicoService) {
        this.parametroServicoService = parametroServicoService;
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.BOX_BANHEIRO_PADRAO;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        CategoriaServico cat = getCategoria();

        BigDecimal arredondamento = parametroServicoService.valor(cat, "ARREDONDAMENTO_MM", BigDecimal.valueOf(50));
        BigDecimal transpasse = parametroServicoService.valor(cat, "TRANSPASSE_MM", BigDecimal.valueOf(50));
        BigDecimal distBordaRoldana = parametroServicoService.valor(cat, "ROLDANA_DIST_BORDA_MM", BigDecimal.valueOf(85));
        BigDecimal distTopoRoldana = parametroServicoService.valor(cat, "ROLDANA_DIST_TOPO_MM", BigDecimal.valueOf(70));
        int roldanasPorFolhaMovel = parametroServicoService.valor(cat, "ROLDANA_QTD_POR_FOLHA_MOVEL", BigDecimal.valueOf(2)).intValue();

        int quantidadeFixas = quantidadeOuZero(entrada.quantidadeFolhasFixas());
        int quantidadeMoveis = quantidadeOuZero(entrada.quantidadeFolhasMoveis());
        if (quantidadeFixas == 0 && quantidadeMoveis == 0) {
            quantidadeFixas = 1;
            quantidadeMoveis = 1;
        }
        int totalFolhas = quantidadeFixas + quantidadeMoveis;

        BigDecimal larguraPorFolha = CalculoUtil.dividir(entrada.larguraVaoMm(), totalFolhas);

        BigDecimal larguraArredondada = CalculoUtil.arredondarParaCima(larguraPorFolha, arredondamento);
        BigDecimal alturaArredondada = CalculoUtil.arredondarParaCima(entrada.alturaVaoMm(), arredondamento);

        List<FolhaCalculada> folhas = new ArrayList<>();
        int numero = 1;
        for (int i = 0; i < quantidadeFixas; i++) {
            String observacao = "Folha " + numero + " de " + totalFolhas + " (Fixa) — Box de Banheiro Padrão: "
                    + "vão " + entrada.larguraVaoMm().stripTrailingZeros().toPlainString() + "mm ÷ " + totalFolhas
                    + " = " + larguraPorFolha.stripTrailingZeros().toPlainString() + "mm, arredondado para "
                    + larguraArredondada.stripTrailingZeros().toPlainString() + "mm. Sem furação de roldana.";
            folhas.add(new FolhaCalculada(TipoFolha.FIXA, larguraArredondada, alturaArredondada, List.of(), List.of(), observacao));
            numero++;
        }
        for (int i = 0; i < quantidadeMoveis; i++) {
            BigDecimal larguraFabricacao = larguraArredondada.add(transpasse);
            List<FuracaoCalculada> furos = gerarFuracoesRoldana(larguraFabricacao, distBordaRoldana, distTopoRoldana, roldanasPorFolhaMovel);

            String observacao = "Folha " + numero + " de " + totalFolhas + " (Móvel) — Box de Banheiro Padrão: "
                    + "\nvão " + entrada.larguraVaoMm().stripTrailingZeros().toPlainString() + "mm ÷ " + totalFolhas
                    + " = " + larguraPorFolha.stripTrailingZeros().toPlainString() + "mm, arredondado para "
                    + larguraArredondada.stripTrailingZeros().toPlainString() + "mm,\n + transpasse "
                    + transpasse.stripTrailingZeros().toPlainString() + "mm = "
                    + larguraFabricacao.stripTrailingZeros().toPlainString() + "mm de fabricação "
                    + roldanasPorFolhaMovel + "\n furo(s) de roldana a " + distBordaRoldana.stripTrailingZeros().toPlainString()
                    + "mm da borda lateral"
                    + (distTopoRoldana == null ? " (distância vertical até o topo não informada no briefing — configure em Parâmetros de Serviço)." : ".");

            folhas.add(new FolhaCalculada(TipoFolha.MOVEL, larguraFabricacao, alturaArredondada, furos, List.of(), observacao));
            numero++;
        }

        return new ResultadoCalculoServico(folhas);
    }

    private int quantidadeOuZero(Integer valor) {
        return valor != null && valor > 0 ? valor : 0;
    }

    private List<FuracaoCalculada> gerarFuracoesRoldana(BigDecimal larguraFolha, BigDecimal distBorda,
                                                          BigDecimal distTopo, int quantidade) {
        List<FuracaoCalculada> furos = new ArrayList<>();

        BigDecimal diametroRoldana = BigDecimal.valueOf(30);

        if (quantidade <= 0 || distBorda == null) {
            return furos;
        }
        if (quantidade == 1) {
            BigDecimal centro = larguraFolha.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            furos.add(new FuracaoCalculada(TipoFuracao.ROLDANA, centro, distTopo, diametroRoldana, "Roldana central"));
            return furos;
        }

        BigDecimal xEsquerda = distBorda;
        BigDecimal xDireita = larguraFolha.subtract(distBorda);

        furos.add(new FuracaoCalculada(
                TipoFuracao.ROLDANA,
                xEsquerda,
                distTopo,
                diametroRoldana,
                "Roldana 1"
        ));

        furos.add(new FuracaoCalculada(
                TipoFuracao.ROLDANA,
                xDireita,
                distTopo,
                diametroRoldana,
                "Roldana 2"
        ));

        if (quantidade > 2) {
            BigDecimal vao = xDireita.subtract(xEsquerda);
            int espacos = quantidade - 1;
            for (int i = 1; i < espacos; i++) {
                BigDecimal x = xEsquerda.add(vao.multiply(BigDecimal.valueOf(i))
                        .divide(BigDecimal.valueOf(espacos), 2, RoundingMode.HALF_UP));
                furos.add(new FuracaoCalculada(TipoFuracao.ROLDANA, x, distTopo, diametroRoldana, "Roldana " + (i + 2)));
            }
        }
        return furos;
    }
}
