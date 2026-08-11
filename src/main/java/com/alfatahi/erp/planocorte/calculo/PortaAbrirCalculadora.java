package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.service.RegraTecnicaService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class PortaAbrirCalculadora implements ServicoCalculadora {

    private static final BigDecimal RECORTE_LARGURA_MM = BigDecimal.valueOf(55);
    private static final BigDecimal RECORTE_ALTURA_MM = BigDecimal.valueOf(140);

    private final RegraTecnicaService regraTecnicaService;

    public PortaAbrirCalculadora(RegraTecnicaService regraTecnicaService) {
        this.regraTecnicaService = regraTecnicaService;
    }

    @Override
    public CategoriaServico getCategoria() {
        return CategoriaServico.PORTA_ABRIR;
    }

    @Override
    public ResultadoCalculoServico calcular(EntradaCalculoServico entrada) {
        List<RegraTecnica> regras = regraTecnicaService.listarAtivasPorCategoria(getCategoria());
        CalculoUtil.AjusteDetalhado ajuste = CalculoUtil.aplicarRegras(entrada.larguraVaoMm(), entrada.alturaVaoMm(), regras);

        boolean ladoDireito = "DIREITO".equalsIgnoreCase(entrada.ladoRecorte());
        boolean ladoInformado = entrada.ladoRecorte() != null && !entrada.ladoRecorte().isBlank();
        String ladoTexto = ladoInformado ? (ladoDireito ? "direito" : "esquerdo") : "a definir";

        List<ElementoTecnico> elementos = new ArrayList<>();
        if (ladoInformado) {
            elementos.add(recorte(ajuste.larguraFinalMm(), ajuste.alturaFinalMm(), ladoDireito, true));
            elementos.add(recorte(ajuste.larguraFinalMm(), ajuste.alturaFinalMm(), ladoDireito, false));
        }

        StringBuilder observacao = new StringBuilder("Porta de Abrir");
        if (!ajuste.resumo().isBlank()) {
            observacao.append(" — ").append(ajuste.resumo());
        }
        observacao.append(" · Recortes das ferragens")
                .append(" no canto superior e no canto inferior, lado ").append(ladoTexto);

        FolhaCalculada folha = new FolhaCalculada(TipoFolha.MOVEL, ajuste.larguraFinalMm(), ajuste.alturaFinalMm(),
                List.of(), elementos, observacao.toString());
        return new ResultadoCalculoServico(List.of(folha));
    }


    private ElementoTecnico recorte(BigDecimal larguraFinalMm, BigDecimal alturaFinalMm, boolean ladoDireito, boolean cantoSuperior) {
        BigDecimal metadeLargura = RECORTE_LARGURA_MM.divide(BigDecimal.valueOf(2));
        BigDecimal metadeAltura = RECORTE_ALTURA_MM.divide(BigDecimal.valueOf(2));

        BigDecimal centroX = ladoDireito ? larguraFinalMm.subtract(metadeLargura) : metadeLargura;
        BigDecimal centroY = cantoSuperior ? metadeAltura : alturaFinalMm.subtract(metadeAltura);

        ElementoTecnico elemento = new ElementoTecnico();
        elemento.setTipo(TipoElemento.RECORTE);
        elemento.setFormato("retangular");
        elemento.setLado((ladoDireito ? "Direito" : "Esquerdo") + " · " + (cantoSuperior ? "superior" : "inferior"));
        elemento.setLarguraMm(RECORTE_LARGURA_MM);
        elemento.setAlturaMm(RECORTE_ALTURA_MM);
        elemento.setPosicaoXMm(centroX);
        elemento.setPosicaoYMm(centroY);
        elemento.setObservacao("Recorte retangular de canto (degrau na borda) — " + RECORTE_LARGURA_MM.stripTrailingZeros().toPlainString()
                + "mm (largura) x " + RECORTE_ALTURA_MM.stripTrailingZeros().toPlainString() + "mm (altura).");
        return elemento;
    }
}
