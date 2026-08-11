package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;







@Service
public class ValidacaoElementoService {

    private final ParametroServicoService parametroServicoService;

    public ValidacaoElementoService(ParametroServicoService parametroServicoService) {
        this.parametroServicoService = parametroServicoService;
    }

    public List<String> validar(PlanoCorteItem item, ElementoTecnico novoElemento, CategoriaServico categoria) {
        List<String> avisos = new ArrayList<>();
        if (item.getLarguraFinalMm() == null || item.getAlturaFinalMm() == null
                || novoElemento.getPosicaoXMm() == null || novoElemento.getPosicaoYMm() == null) {
            return avisos;
        }

        BigDecimal distanciaMinimaBorda = parametroServicoService.valor(
                categoria, "DISTANCIA_MINIMA_BORDA_MM", BigDecimal.valueOf(15));

        BigDecimal largura = item.getLarguraFinalMm();
        BigDecimal altura = item.getAlturaFinalMm();
        BigDecimal x = novoElemento.getPosicaoXMm();
        BigDecimal y = novoElemento.getPosicaoYMm();
        BigDecimal raio = raioAproximado(novoElemento);

        if (x.signum() < 0 || y.signum() < 0 || x.compareTo(largura) > 0 || y.compareTo(altura) > 0) {
            avisos.add("O elemento está fora da área da peça (peça " + largura + "×" + altura + "mm).");
        }

        BigDecimal distEsquerda = x;
        BigDecimal distDireita = largura.subtract(x);
        BigDecimal distTopo = y;
        BigDecimal distFundo = altura.subtract(y);
        BigDecimal distBordaMaisProxima = min(distEsquerda, distDireita, distTopo, distFundo).subtract(raio);

        if (distBordaMaisProxima.compareTo(distanciaMinimaBorda) < 0) {
            avisos.add("O " + novoElemento.getTipo().getDescricao().toLowerCase()
                    + " está a aproximadamente " + arredondar(distBordaMaisProxima) + "mm da borda mais próxima. "
                    + "A distância mínima parametrizada é de " + distanciaMinimaBorda + "mm.");
        }

        for (var furo : item.getFuracoes()) {
            if (furo.getPosicaoXMm() == null || furo.getPosicaoYMm() == null) {
                continue;
            }
            if (distancia(x, y, furo.getPosicaoXMm(), furo.getPosicaoYMm()).compareTo(raio.add(BigDecimal.valueOf(5))) < 0) {
                avisos.add("Este elemento fica muito próximo de outro furo já existente (" + furo.getDescricao() + ").");
            }
        }
        for (ElementoTecnico existente : item.getElementos()) {
            if (existente.getPosicaoXMm() == null || existente.getPosicaoYMm() == null) {
                continue;
            }
            BigDecimal raioExistente = raioAproximado(existente);
            if (distancia(x, y, existente.getPosicaoXMm(), existente.getPosicaoYMm())
                    .compareTo(raio.add(raioExistente)) < 0) {
                avisos.add("Este elemento se sobrepõe a outro elemento já cadastrado nesta peça.");
            }
        }

        return avisos;
    }

    private BigDecimal raioAproximado(ElementoTecnico elemento) {
        if (elemento.getTipo() == TipoElemento.FURO && elemento.getDiametroMm() != null) {
            return elemento.getDiametroMm().divide(BigDecimal.valueOf(2));
        }
        if (elemento.getRaioMm() != null) {
            return elemento.getRaioMm();
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal distancia(BigDecimal x1, BigDecimal y1, BigDecimal x2, BigDecimal y2) {
        double dx = x1.subtract(x2).doubleValue();
        double dy = y1.subtract(y2).doubleValue();
        return BigDecimal.valueOf(Math.sqrt(dx * dx + dy * dy));
    }

    private BigDecimal min(BigDecimal... valores) {
        BigDecimal menor = valores[0];
        for (BigDecimal v : valores) {
            if (v.compareTo(menor) < 0) {
                menor = v;
            }
        }
        return menor;
    }

    private BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(1, java.math.RoundingMode.HALF_UP);
    }
}
