package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.service.ParametroServicoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SacadaCalculadora implements ServicoCalculadora {

    private final ParametroServicoService parametros;

    public SacadaCalculadora(ParametroServicoService parametros) {
        this.parametros = parametros;
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.SACADA;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        CategoriaServico cat = getCategoria();
        BigDecimal descontoAltura = parametros.valor(cat, "DESCONTO_ALTURA_MM", BigDecimal.valueOf(170));
        BigDecimal descontoEsquerdo = parametros.valor(cat, "DESCONTO_LATERAL_ESQUERDO_MM", BigDecimal.valueOf(100));
        BigDecimal descontoDireito = parametros.valor(cat, "DESCONTO_LATERAL_DIREITO_MM", BigDecimal.valueOf(100));
        BigDecimal descontoEntreFolhas = parametros.valor(cat, "DESCONTO_ENTRE_FOLHAS_MM", BigDecimal.valueOf(3));

        int quantidadeFolhas = CalculoUtil.folhasOuPadrao(entrada.quantidadeFolhas(), 1);

        BigDecimal altura = CalculoUtil.descontar(entrada.alturaVaoMm(), descontoAltura);

        BigDecimal larguraUtil = CalculoUtil.descontar(entrada.larguraVaoMm(), descontoEsquerdo.add(descontoDireito));
        BigDecimal larguraPorFolha = CalculoUtil.dividir(larguraUtil, quantidadeFolhas);

        boolean variasFolhas = quantidadeFolhas > 1;
        BigDecimal larguraFinalFolha = variasFolhas
                ? CalculoUtil.descontar(larguraPorFolha, descontoEntreFolhas)
                : larguraPorFolha;

        List<FolhaCalculada> folhas = new ArrayList<>();
        for (int i = 1; i <= quantidadeFolhas; i++) {
            StringBuilder observacao = new StringBuilder("Folha " + i + " de " + quantidadeFolhas + " — Sacada. ");
            observacao.append("Largura bruta ").append(entrada.larguraVaoMm().stripTrailingZeros().toPlainString()).append("mm");
            observacao.append(" − ").append(descontoEsquerdo.stripTrailingZeros().toPlainString()).append("mm (desconto lateral esquerdo)");
            observacao.append(" − ").append(descontoDireito.stripTrailingZeros().toPlainString()).append("mm (desconto lateral direito)");
            observacao.append(" = ").append(larguraUtil.stripTrailingZeros().toPlainString()).append("mm úteis ÷ ").append(quantidadeFolhas).append(" folha(s) = ")
                    .append(larguraPorFolha.stripTrailingZeros().toPlainString()).append("mm");
            if (variasFolhas) {
                observacao.append(" − ").append(descontoEntreFolhas.stripTrailingZeros().toPlainString())
                        .append("mm (espaço entre folhas) = ").append(larguraFinalFolha.stripTrailingZeros().toPlainString()).append("mm");
            }
            observacao.append(" · Altura bruta ").append(entrada.alturaVaoMm().stripTrailingZeros().toPlainString())
                    .append("mm − ").append(descontoAltura.stripTrailingZeros().toPlainString()).append("mm = ")
                    .append(altura.stripTrailingZeros().toPlainString()).append("mm");

            folhas.add(new FolhaCalculada(TipoFolha.FIXA, larguraFinalFolha, altura, List.of(), List.of(), observacao.toString()));
        }
        return new ResultadoCalculoServico(folhas);
    }
}
