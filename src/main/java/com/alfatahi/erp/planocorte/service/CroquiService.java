package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.entity.Anotacao;
import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.Furacao;
import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.TipoAncoragem;
import com.alfatahi.erp.planocorte.entity.TipoAnotacao;
import com.alfatahi.erp.planocorte.entity.TipoBorda;
import com.alfatahi.erp.planocorte.entity.TipoElemento;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import com.alfatahi.erp.planocorte.dto.CroquiVaoChunkDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CroquiService {

    private static final double CANVAS_LARGURA_PX = 800;
    private static final double CANVAS_ALTURA_PX = 600;
    private static final double MARGEM_PX = 60;
    private static final double TITULO_PX = 44;
    private static final double TOLERANCIA_PAR_MM = 5;

    private static final double PECA_MIN_PX = 220;

    private static final double EIXO_DUPLO_EXTRA_PX = 44;

    private static final String FONTE = "Helvetica, Arial, sans-serif";

    public String gerarSvg(PlanoCorteItem item) {
        double larguraMm = item.getLarguraFinalMm().doubleValue();
        double alturaMm = item.getAlturaFinalMm().doubleValue();

        if (item.isDimensoesPersonalizadas()) {
            GeometriaPersonalizada geometria = calcularGeometriaPersonalizada(item);
            larguraMm = geometria.larguraMm();
            alturaMm = geometria.alturaMm();
        }

        double escalaLargura = larguraMm > 0 ? CANVAS_LARGURA_PX / larguraMm : 1;
        double escalaAltura = alturaMm > 0 ? CANVAS_ALTURA_PX / alturaMm : 1;
        double escala = Math.min(escalaLargura, escalaAltura);

        double larguraPx = Math.max(larguraMm * escala, PECA_MIN_PX);
        double alturaPx = Math.max(alturaMm * escala, PECA_MIN_PX);

        boolean trapezoidal = item.isDimensoesPersonalizadas();
        double margemDireita = trapezoidal ? MARGEM_PX + EIXO_DUPLO_EXTRA_PX : MARGEM_PX;

        double x0 = MARGEM_PX;
        double y0 = MARGEM_PX + TITULO_PX;
        double svgW = larguraPx + MARGEM_PX + margemDireita;
        double svgH = alturaPx + MARGEM_PX * 2 + TITULO_PX;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" viewBox=\"0 0 %.0f %.0f\" preserveAspectRatio=\"xMidYMid meet\" class=\"croqui-svg\" data-item-id=\"%s\" data-origin-x=\"%.3f\" data-origin-y=\"%.3f\" data-escala=\"%.6f\">",
                svgW, svgH, svgW, svgH, item.getId(), x0, y0, escala));
        svg.append(defsSetas());

        svg.append(String.format(Locale.US,
                "<text x=\"%.0f\" y=\"20\" font-family=\"%s\" font-size=\"16\" font-weight=\"700\" fill=\"#0f172a\" letter-spacing=\"0.4\">%s</text>",
                MARGEM_PX, FONTE, escapeXml(item.getCodigoPeca())));

        String titulo = item.getVidroNomeSnapshot() + " · " + formatarNumero(item.getEspessuraSnapshot()) + "mm";
        if (item.isRedondo()) {
            titulo += " · Redondo";
        }
        if (item.getTipoFolha() != null && item.getTipoFolha() != TipoFolha.UNICA) {
            titulo += " · " + item.getTipoFolha().getDescricao();
        }
        svg.append(String.format(Locale.US,
                "<text x=\"%.0f\" y=\"39\" font-family=\"%s\" font-size=\"13\" fill=\"#64748b\">%s</text>",
                MARGEM_PX, FONTE, escapeXml(titulo)));

        desenharPeca(svg, item, x0, y0, larguraPx, alturaPx, escala, !trapezoidal, true, trapezoidal);
        if (trapezoidal) {
            desenharCotasPersonalizadas(svg, item, x0, y0 + alturaPx, escala);
        }

        if (item.getObservacoes() != null && !item.getObservacoes().isBlank()) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.0f\" y=\"%.0f\" font-family=\"%s\" font-size=\"12\" fill=\"#64748b\">%s</text>",
                    MARGEM_PX, svgH - 8, FONTE, escapeXml(resumo(item.getObservacoes(), 60))));
        }

        svg.append("</svg>");
        return svg.toString();
    }


    public String gerarSvgVao(List<PlanoCorteItem> folhas) {
        return gerarSvgVao(folhas, 0, folhas != null ? folhas.size() : 0);
    }

    public String gerarSvgVao(List<PlanoCorteItem> folhas, int offsetNumeroFolha, int totalFolhasVao) {
        if (folhas == null || folhas.isEmpty()) {
            return "";
        }
        if (folhas.size() == 1 && totalFolhasVao <= 1) {
            return gerarSvg(folhas.get(0));
        }

        double alturaMaxMm = folhas.stream()
                .mapToDouble(f -> f.getAlturaFinalMm() != null ? f.getAlturaFinalMm().doubleValue() : 0)
                .max().orElse(0);
        double alturaMinMm = folhas.stream()
                .mapToDouble(f -> f.getAlturaFinalMm() != null ? f.getAlturaFinalMm().doubleValue() : 0)
                .min().orElse(0);

        boolean alturasDiferentes = Math.abs(alturaMaxMm - alturaMinMm) > TOLERANCIA_PAR_MM;

        PlanoCorteItem primeira = folhas.get(0);
        boolean trapezoidal = primeira.isDimensoesPersonalizadas();

        double somaLarguraMm = folhas.stream()
                .mapToDouble(f -> f.getLarguraFinalMm() != null ? f.getLarguraFinalMm().doubleValue() : 0)
                .sum();

        double gapMm = 60;
        double larguraDesenhoTotalMm = somaLarguraMm + gapMm * (folhas.size() - 1);

        double maxLarguraPx = Math.max(260 * folhas.size(), CANVAS_LARGURA_PX);
        double maxAlturaPx = CANVAS_ALTURA_PX;
        double escalaLargura = larguraDesenhoTotalMm > 0 ? maxLarguraPx / larguraDesenhoTotalMm : 1;
        double escalaAltura = alturaMaxMm > 0 ? maxAlturaPx / alturaMaxMm : 1;
        double escala = Math.min(escalaLargura, escalaAltura);

        double gapPx = gapMm * escala;
        if (alturasDiferentes && !trapezoidal) {

            gapPx = Math.max(gapPx, 80);
        }
        double alturaPecaPx = Math.max(alturaMaxMm * escala, PECA_MIN_PX);


        double margemEsquerda = MARGEM_PX;
        double margemDireita = trapezoidal ? MARGEM_PX + EIXO_DUPLO_EXTRA_PX : MARGEM_PX;

        double y0 = MARGEM_PX + TITULO_PX + 22;
        double svgH = alturaPecaPx + MARGEM_PX * 2 + TITULO_PX + 22 + 34 + (trapezoidal ? 34 : 0);

        double larguraTotalPx = 0;
        for (int i = 0; i < folhas.size(); i++) {
            PlanoCorteItem folha = folhas.get(i);
            double larguraFolhaPx = Math.max((folha.getLarguraFinalMm() != null ? folha.getLarguraFinalMm().doubleValue() : 0) * escala, PECA_MIN_PX);
            larguraTotalPx += larguraFolhaPx;
            if (i < folhas.size() - 1) {
                larguraTotalPx += gapPx;
            }
        }
        double svgW = larguraTotalPx + margemEsquerda + margemDireita;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" viewBox=\"0 0 %.0f %.0f\" preserveAspectRatio=\"xMidYMid meet\">",
                svgW, svgH, svgW, svgH));
        svg.append(defsSetas());

        String rotuloVao = primeira.getGrupoVao() != null ? "Vão " + primeira.getGrupoVao() : "Vão";
        svg.append(String.format(Locale.US,
                "<text x=\"%.0f\" y=\"20\" font-family=\"%s\" font-size=\"16\" font-weight=\"700\" fill=\"#0f172a\" letter-spacing=\"0.4\">%s</text>",
                margemEsquerda, FONTE, escapeXml(rotuloVao)));

        String titulo = totalFolhasVao + " folhas · " + primeira.getVidroNomeSnapshot() + " · "
                + formatarNumero(primeira.getEspessuraSnapshot()) + "mm";
        svg.append(String.format(Locale.US,
                "<text x=\"%.0f\" y=\"39\" font-family=\"%s\" font-size=\"13\" fill=\"#64748b\">%s</text>",
                margemEsquerda, FONTE, escapeXml(titulo)));

        boolean desenharCotaAlturaPorFolha = alturasDiferentes && !trapezoidal;

        double xInicio = margemEsquerda;
        double cursorX = xInicio;
        for (int i = 0; i < folhas.size(); i++) {
            PlanoCorteItem folha = folhas.get(i);
            double larguraFolhaMm = folha.getLarguraFinalMm() != null ? folha.getLarguraFinalMm().doubleValue() : 0;
            double alturaFolhaMm = folha.getAlturaFinalMm() != null ? folha.getAlturaFinalMm().doubleValue() : 0;
            double larguraFolhaPx = Math.max(larguraFolhaMm * escala, PECA_MIN_PX);
            double alturaFolhaPx = Math.max(alturaFolhaMm * escala, PECA_MIN_PX);
            double folhaY0 = y0 + (alturaPecaPx - alturaFolhaPx);

            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"11\" font-weight=\"700\" fill=\"#4338ca\" text-anchor=\"middle\">%s</text>",
                    cursorX + larguraFolhaPx / 2, y0 - 24, FONTE, escapeXml("Folha " + (offsetNumeroFolha + i + 1))));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#94a3b8\" text-anchor=\"middle\">%s</text>",
                    cursorX + larguraFolhaPx / 2, y0 - 12, FONTE, escapeXml(folha.getCodigoPeca())));

            desenharPeca(svg, folha, cursorX, folhaY0, larguraFolhaPx, alturaFolhaPx, escala,
                    true, desenharCotaAlturaPorFolha, false);

            cursorX += larguraFolhaPx + gapPx;
        }
        if (trapezoidal) {
            desenharContornoTrapezoidalVao(svg, primeira, xInicio, y0 + alturaPecaPx, escala);
            desenharCotasPersonalizadas(svg, primeira, xInicio, y0 + alturaPecaPx, escala);
        } else if (!alturasDiferentes) {


            desenharCotaAlturaVertical(svg, xInicio - 24, y0, alturaPecaPx,
                    BigDecimal.valueOf(alturaMaxMm), "#334155", -16);
        }



        svg.append("</svg>");
        return svg.toString();
    }

    public List<CroquiVaoChunkDto> gerarSvgsVaoAgrupados(
            List<PlanoCorteItem> folhas, int maxFolhasPorCroqui) {

        List<CroquiVaoChunkDto> resultado = new ArrayList<>();
        if (folhas == null || folhas.isEmpty()) {
            return resultado;
        }

        int total = folhas.size();
        int tamanhoChunk = maxFolhasPorCroqui > 0 ? maxFolhasPorCroqui : total;

        for (int inicio = 0; inicio < total; inicio += tamanhoChunk) {
            int fim = Math.min(inicio + tamanhoChunk, total);
            List<PlanoCorteItem> chunk = folhas.subList(inicio, fim);
            String svgChunk = gerarSvgVao(chunk, inicio, total);
            resultado.add(new CroquiVaoChunkDto(svgChunk, inicio + 1, fim, total));
        }

        return resultado;
    }


    private void desenharPeca(StringBuilder svg, PlanoCorteItem item, double x0, double y0,
                               double larguraPx, double alturaPx, double escala,
                               boolean desenharCotasPadrao, boolean desenharCotaAltura,
                               boolean contornoTrapezoidal) {
        boolean redondo = item.isRedondo();
        if (redondo) {
            desenharContornoRedondo(svg, item, x0, y0, larguraPx, alturaPx, escala);
            desenharBisoteRedondo(svg, item, x0, y0, larguraPx, alturaPx, escala);
        } else if (contornoTrapezoidal) {
            desenharContornoTrapezoidal(svg, item, x0, y0, larguraPx, alturaPx, escala);
            desenharBisoteTrapezoidal(svg, item, x0, y0, larguraPx, alturaPx, escala);
        } else {
            desenharContornoPeca(svg, item, x0, y0, larguraPx, alturaPx, escala);
            desenharBisote(svg, item, x0, y0, larguraPx, alturaPx, escala);
        }

        desenharFuracoes(svg, item.getFuracoes(), x0, y0, escala);
        List<GeometriaElemento> geometriasElementos = item.getElementos().stream()
                .map(elemento -> calcularGeometriaElemento(elemento, x0, y0, escala))
                .toList();
        List<LayoutCotaElemento> layoutsCotas = planejarCotasElementos(
                item.getElementos(), geometriasElementos, x0, y0, larguraPx, alturaPx, escala);
        for (int indice = 0; indice < item.getElementos().size(); indice++) {
            desenharElemento(svg, item.getElementos().get(indice), geometriasElementos.get(indice),
                    layoutsCotas.get(indice), escala);
        }
        desenharAnotacoes(svg, item.getAnotacoes(), x0, y0, escala);

        if (!desenharCotasPadrao) {
            return;
        }

        double cotaY = y0 + alturaPx + 26;
        if (redondo) {
            svg.append(linhaCota(x0, cotaY, x0 + larguraPx, cotaY));
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"13\" font-weight=\"600\" fill=\"#334155\" text-anchor=\"middle\">Ø %s mm</text>",
                    x0 + larguraPx / 2, cotaY + 18, FONTE, formatarNumero(item.getLarguraFinalMm())));
            return;
        }

        svg.append(linhaCota(x0, cotaY, x0 + larguraPx, cotaY));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"13\" font-weight=\"600\" fill=\"#334155\" text-anchor=\"middle\">%s mm</text>",
                x0 + larguraPx / 2, cotaY + 18, FONTE, formatarNumero(item.getLarguraFinalMm())));

        if (desenharCotaAltura) {
            desenharCotaAlturaVertical(svg, x0 - 24, y0, alturaPx, item.getAlturaFinalMm(), "#334155", -16);
        }
    }


    private void desenharCotaAlturaVertical(StringBuilder svg, double cotaX, double y0, double alturaPx,
                                             BigDecimal valorMm, String corTexto, double offsetLabelPx) {
        svg.append(linhaCota(cotaX, y0, cotaX, y0 + alturaPx));
        svg.append(String.format(Locale.US,
                "<text x=\"0\" y=\"0\" font-family=\"%s\" font-size=\"13\" font-weight=\"600\" fill=\"%s\" text-anchor=\"middle\" transform=\"translate(%.1f %.1f) rotate(-90)\">%s mm</text>",
                FONTE, corTexto, cotaX + offsetLabelPx, y0 + alturaPx / 2, formatarNumero(valorMm)));
    }

    private void desenharCotasPersonalizadas(StringBuilder svg, PlanoCorteItem item,
                                              double xEsquerda, double yBase, double escala) {
        GeometriaPersonalizada geometria = calcularGeometriaPersonalizada(item);
        List<Ponto> pontos = converterParaPixels(geometria, xEsquerda, yBase, escala);
        double offsetCota = 30;
        double centroX = pontos.stream().mapToDouble(Ponto::x).average().orElse(xEsquerda);
        double centroY = pontos.stream().mapToDouble(Ponto::y).average().orElse(yBase);

        for (int i = 0; i < pontos.size(); i++) {
            Ponto inicio = pontos.get(i);
            Ponto fim = pontos.get((i + 1) % pontos.size());
            desenharCotaLado(svg, inicio.x(), inicio.y(), fim.x(), fim.y(),
                    offsetCota, centroX, centroY, geometria.medidasLadosMm().get(i));
        }
    }

    private void desenharCotaLado(StringBuilder svg, double x1, double y1, double x2, double y2,
                                   double offset, double centroX, double centroY, BigDecimal valorMm) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double comprimento = Math.sqrt(dx * dx + dy * dy);
        if (comprimento < 0.5) {
            return;
        }

        double perpX = -dy / comprimento;
        double perpY = dx / comprimento;

        double midSegX = (x1 + x2) / 2;
        double midSegY = (y1 + y2) / 2;
        double paraCentroX = centroX - midSegX;
        double paraCentroY = centroY - midSegY;
        double produtoEscalar = perpX * paraCentroX + perpY * paraCentroY;
        double sinal = produtoEscalar > 0 ? -1 : 1;

        double offX = perpX * offset * sinal;
        double offY = perpY * offset * sinal;

        double cx1 = x1 + offX;
        double cy1 = y1 + offY;
        double cx2 = x2 + offX;
        double cy2 = y2 + offY;

        svg.append(linhaCota(cx1, cy1, cx2, cy2));

        svg.append(String.format(Locale.US,
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#cbd5e1\" stroke-width=\"1\" stroke-dasharray=\"3 3\"/>",
                x1, y1, cx1, cy1));
        svg.append(String.format(Locale.US,
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#cbd5e1\" stroke-width=\"1\" stroke-dasharray=\"3 3\"/>",
                x2, y2, cx2, cy2));

        double midX = (cx1 + cx2) / 2;
        double midY = (cy1 + cy2) / 2;
        double anguloGraus = Math.toDegrees(Math.atan2(dy, dx));

        if (anguloGraus > 90) {
            anguloGraus -= 180;
        } else if (anguloGraus < -90) {
            anguloGraus += 180;
        }

        double textoOffX = perpX * 13 * sinal;
        double textoOffY = perpY * 13 * sinal;

        svg.append(String.format(Locale.US,
                "<text x=\"0\" y=\"0\" font-family=\"%s\" font-size=\"13\" font-weight=\"600\" fill=\"#334155\" text-anchor=\"middle\" transform=\"translate(%.1f %.1f) rotate(%.2f)\">%s mm</text>",
                FONTE, midX + textoOffX, midY + textoOffY, anguloGraus, formatarNumero(valorMm)));
    }


    private BigDecimal valorFinalOuBruta(BigDecimal finalMm, BigDecimal brutaMm) {
        return finalMm != null ? finalMm : brutaMm;
    }


    private void desenharContornoTrapezoidal(StringBuilder svg, PlanoCorteItem item, double x0, double y0,
                                              double larguraPx, double alturaPx, double escala) {
        double yBase = y0 + alturaPx;
        List<Ponto> pontos = converterParaPixels(calcularGeometriaPersonalizada(item), x0, yBase, escala);
        svg.append(String.format(Locale.US,
                "<polygon points=\"%s\" fill=\"#eff6ff\" stroke=\"#1e293b\" stroke-width=\"2\"/>",
                formatarPontos(pontos)));
    }


    private void desenharContornoTrapezoidalVao(StringBuilder svg, PlanoCorteItem primeira,
                                                 double xEsquerda, double yBase, double escala) {
        List<Ponto> pontos = converterParaPixels(calcularGeometriaPersonalizada(primeira), xEsquerda, yBase, escala);
        svg.append(String.format(Locale.US,
                "<polygon points=\"%s\" fill=\"none\" stroke=\"#7c3aed\" stroke-width=\"2\" stroke-dasharray=\"7 5\"/>",
                formatarPontos(pontos)));
    }

    private String pontosTrapezio(double xEsquerda, double yBase,
                                   double alturaEsquerdaPx, double alturaDireitaPx,
                                   double larguraSuperiorPx, double larguraInferiorPx) {
        double xTL = xEsquerda;
        double yTL = yBase - alturaEsquerdaPx;
        double xTR = xEsquerda + larguraSuperiorPx;
        double yTR = yBase - alturaDireitaPx;
        double xBR = xEsquerda + larguraInferiorPx;
        double xBL = xEsquerda;
        return String.format(Locale.US, "%.1f,%.1f %.1f,%.1f %.1f,%.1f %.1f,%.1f",
                xTL, yTL, xTR, yTR, xBR, yBase, xBL, yBase);
    }

    private GeometriaPersonalizada calcularGeometriaPersonalizada(PlanoCorteItem item) {
        BigDecimal alturaEsquerda = valorFinalOuBruta(
                item.getAlturaFinalEsquerdaMm(), item.getAlturaBrutaEsquerdaMm());
        BigDecimal alturaDireita = valorFinalOuBruta(
                item.getAlturaFinalDireitaMm(), item.getAlturaBrutaDireitaMm());
        BigDecimal larguraSuperior = valorFinalOuBruta(
                item.getLarguraFinalSuperiorMm(), item.getLarguraBrutaSuperiorMm());
        BigDecimal larguraInferior = valorFinalOuBruta(
                item.getLarguraFinalInferiorMm(), item.getLarguraBrutaInferiorMm());

        int medidasZeradas = contarZeros(
                alturaEsquerda, alturaDireita, larguraSuperior, larguraInferior);
        if (medidasZeradas == 1) {
            GeometriaPersonalizada triangulo = calcularGeometriaTriangular(
                    alturaEsquerda, alturaDireita, larguraSuperior, larguraInferior);
            if (triangulo != null) {
                return triangulo;
            }
        }

        return criarGeometria(
                List.of(
                        new Ponto(0, alturaEsquerda.doubleValue()),
                        new Ponto(larguraSuperior.doubleValue(), alturaDireita.doubleValue()),
                        new Ponto(larguraInferior.doubleValue(), 0),
                        new Ponto(0, 0)
                ),
                List.of(larguraSuperior, alturaDireita, larguraInferior, alturaEsquerda));
    }

    private GeometriaPersonalizada calcularGeometriaTriangular(
            BigDecimal alturaEsquerda, BigDecimal alturaDireita,
            BigDecimal larguraSuperior, BigDecimal larguraInferior) {
        if (ehZero(alturaEsquerda)) {
            return criarTrianguloBaseInferior(
                    larguraSuperior, alturaDireita, larguraInferior, TipoVerticeTriangular.BASE_ESQUERDA);
        }
        if (ehZero(larguraSuperior)) {
            return criarTrianguloBaseInferior(
                    alturaEsquerda, alturaDireita, larguraInferior, TipoVerticeTriangular.APICE);
        }
        if (ehZero(alturaDireita)) {
            return criarTrianguloBaseInferior(
                    alturaEsquerda, larguraSuperior, larguraInferior, TipoVerticeTriangular.APICE);
        }
        if (ehZero(larguraInferior)) {
            Ponto apiceInferior = calcularApice(alturaEsquerda, alturaDireita, larguraSuperior);
            if (apiceInferior == null) {
                return null;
            }
            double altura = apiceInferior.y();
            return criarGeometria(
                    List.of(
                            new Ponto(0, altura),
                            new Ponto(larguraSuperior.doubleValue(), altura),
                            new Ponto(apiceInferior.x(), 0)
                    ),
                    List.of(larguraSuperior, alturaDireita, alturaEsquerda));
        }
        return null;
    }

    private GeometriaPersonalizada criarTrianguloBaseInferior(
            BigDecimal ladoEsquerdo, BigDecimal ladoDireito, BigDecimal base,
            TipoVerticeTriangular primeiroVertice) {
        Ponto apice = calcularApice(ladoEsquerdo, ladoDireito, base);
        if (apice == null) {
            return null;
        }

        Ponto baseEsquerda = new Ponto(0, 0);
        Ponto baseDireita = new Ponto(base.doubleValue(), 0);
        if (primeiroVertice == TipoVerticeTriangular.BASE_ESQUERDA) {
            return criarGeometria(
                    List.of(baseEsquerda, apice, baseDireita),
                    List.of(ladoEsquerdo, ladoDireito, base));
        }
        return criarGeometria(
                List.of(apice, baseDireita, baseEsquerda),
                List.of(ladoDireito, base, ladoEsquerdo));
    }

    private Ponto calcularApice(BigDecimal ladoEsquerdo, BigDecimal ladoDireito, BigDecimal base) {
        if (!formaTrianguloValido(ladoEsquerdo, ladoDireito, base)) {
            return null;
        }
        double esquerdo = ladoEsquerdo.doubleValue();
        double direito = ladoDireito.doubleValue();
        double baseMm = base.doubleValue();
        double x = (esquerdo * esquerdo - direito * direito + baseMm * baseMm) / (2 * baseMm);
        double alturaAoQuadrado = esquerdo * esquerdo - x * x;
        return new Ponto(x, Math.sqrt(Math.max(alturaAoQuadrado, 0)));
    }

    private boolean formaTrianguloValido(BigDecimal primeiro, BigDecimal segundo, BigDecimal terceiro) {
        return primeiro.signum() > 0 && segundo.signum() > 0 && terceiro.signum() > 0
                && primeiro.add(segundo).compareTo(terceiro) > 0
                && primeiro.add(terceiro).compareTo(segundo) > 0
                && segundo.add(terceiro).compareTo(primeiro) > 0;
    }

    private int contarZeros(BigDecimal... medidas) {
        int quantidade = 0;
        for (BigDecimal medida : medidas) {
            if (ehZero(medida)) {
                quantidade++;
            }
        }
        return quantidade;
    }

    private boolean ehZero(BigDecimal medida) {
        return medida.signum() == 0;
    }

    private GeometriaPersonalizada criarGeometria(
            List<Ponto> vertices, List<BigDecimal> medidasLadosMm) {
        double minX = vertices.stream().mapToDouble(Ponto::x).min().orElse(0);
        double minY = vertices.stream().mapToDouble(Ponto::y).min().orElse(0);
        double maxX = vertices.stream().mapToDouble(Ponto::x).max().orElse(0);
        double maxY = vertices.stream().mapToDouble(Ponto::y).max().orElse(0);
        List<Ponto> normalizados = vertices.stream()
                .map(ponto -> new Ponto(ponto.x() - minX, ponto.y() - minY))
                .toList();
        return new GeometriaPersonalizada(
                normalizados, medidasLadosMm, maxX - minX, maxY - minY);
    }

    private List<Ponto> converterParaPixels(
            GeometriaPersonalizada geometria, double xEsquerda, double yBase, double escala) {
        return geometria.verticesMm().stream()
                .map(ponto -> new Ponto(
                        xEsquerda + ponto.x() * escala,
                        yBase - ponto.y() * escala))
                .toList();
    }

    private void desenharBisoteTrapezoidal(StringBuilder svg, PlanoCorteItem item, double x0, double y0,
                                            double larguraPx, double alturaPx, double escala) {
        if (item.getEspessuraBisoteMm() == null || item.getEspessuraBisoteMm().signum() <= 0) {
            return;
        }
        double faixaPx = Math.max(item.getEspessuraBisoteMm().doubleValue() * escala, 2);

        double yBase = y0 + alturaPx;
        List<Ponto> pontosExternos = converterParaPixels(calcularGeometriaPersonalizada(item), x0, yBase, escala);
        List<Ponto> pontosInternos = calcularContornoInterno(pontosExternos, faixaPx);
        if (pontosInternos.size() < 3) {
            return;
        }

        svg.append(String.format(Locale.US,
                "<polygon points=\"%s\" fill=\"none\" stroke=\"#0ea5e9\" stroke-width=\"1.3\" stroke-dasharray=\"5 3\"/>",
                formatarPontos(pontosInternos)));

        Ponto primeiroPontoInterno = pontosInternos.get(0);
        double cotaY = primeiroPontoInterno.y() + Math.min(faixaPx, alturaPx / 4) + 15;
        svg.append(linhaCota(pontosExternos.get(0).x(), cotaY, primeiroPontoInterno.x(), cotaY));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" fill=\"#0369a1\" text-anchor=\"start\">Bisô %s mm</text>",
                primeiroPontoInterno.x() + 5, cotaY + 4, FONTE, formatarNumero(item.getEspessuraBisoteMm())));
    }

    private List<Ponto> calcularContornoInterno(List<Ponto> pontos, double recuoPx) {
        List<Ponto> contorno = removerPontosConsecutivosDuplicados(pontos);
        if (contorno.size() < 3) {
            return List.of();
        }

        double areaDobrada = 0;
        for (int i = 0; i < contorno.size(); i++) {
            Ponto atual = contorno.get(i);
            Ponto proximo = contorno.get((i + 1) % contorno.size());
            areaDobrada += atual.x() * proximo.y() - proximo.x() * atual.y();
        }
        if (Math.abs(areaDobrada) < 0.000001) {
            return List.of();
        }

        double sentidoInterno = areaDobrada > 0 ? 1 : -1;
        List<Reta> ladosDeslocados = new ArrayList<>();
        for (int i = 0; i < contorno.size(); i++) {
            Ponto inicio = contorno.get(i);
            Ponto fim = contorno.get((i + 1) % contorno.size());
            double dx = fim.x() - inicio.x();
            double dy = fim.y() - inicio.y();
            double comprimento = Math.hypot(dx, dy);
            if (comprimento < 0.000001) {
                return List.of();
            }

            double normalX = -dy / comprimento * sentidoInterno;
            double normalY = dx / comprimento * sentidoInterno;
            ladosDeslocados.add(new Reta(
                    new Ponto(inicio.x() + normalX * recuoPx, inicio.y() + normalY * recuoPx),
                    dx, dy));
        }

        List<Ponto> resultado = new ArrayList<>();
        for (int i = 0; i < ladosDeslocados.size(); i++) {
            Reta anterior = ladosDeslocados.get((i - 1 + ladosDeslocados.size()) % ladosDeslocados.size());
            Reta atual = ladosDeslocados.get(i);
            Ponto intersecao = intersecao(anterior, atual);
            if (intersecao == null) {
                return List.of();
            }
            resultado.add(intersecao);
        }
        return resultado;
    }


    private List<Ponto> removerPontosConsecutivosDuplicados(List<Ponto> pontos) {
        List<Ponto> resultado = new ArrayList<>();
        for (Ponto ponto : pontos) {
            if (resultado.isEmpty() || distancia(resultado.get(resultado.size() - 1), ponto) >= 0.000001) {
                resultado.add(ponto);
            }
        }
        if (resultado.size() > 1 && distancia(resultado.get(0), resultado.get(resultado.size() - 1)) < 0.000001) {
            resultado.remove(resultado.size() - 1);
        }
        return resultado;
    }


    private double distancia(Ponto primeiro, Ponto segundo) {
        return Math.hypot(segundo.x() - primeiro.x(), segundo.y() - primeiro.y());
    }


    private Ponto intersecao(Reta primeira, Reta segunda) {
        double denominador = primeira.dx() * segunda.dy() - primeira.dy() * segunda.dx();
        if (Math.abs(denominador) < 0.000001) {
            return null;
        }

        double entreOrigensX = segunda.origem().x() - primeira.origem().x();
        double entreOrigensY = segunda.origem().y() - primeira.origem().y();
        double parametro = (entreOrigensX * segunda.dy() - entreOrigensY * segunda.dx()) / denominador;
        return new Ponto(
                primeira.origem().x() + parametro * primeira.dx(),
                primeira.origem().y() + parametro * primeira.dy());
    }


    private String formatarPontos(List<Ponto> pontos) {
        StringBuilder resultado = new StringBuilder();
        for (Ponto ponto : pontos) {
            if (!resultado.isEmpty()) {
                resultado.append(' ');
            }
            resultado.append(String.format(Locale.US, "%.1f,%.1f", ponto.x(), ponto.y()));
        }
        return resultado.toString();
    }


    private record GeometriaPersonalizada(
            List<Ponto> verticesMm, List<BigDecimal> medidasLadosMm,
            double larguraMm, double alturaMm) {
    }

    private enum TipoVerticeTriangular {
        BASE_ESQUERDA, APICE
    }

    private record Ponto(double x, double y) {
    }


    private record Reta(Ponto origem, double dx, double dy) {
    }


    private void desenharContornoRedondo(StringBuilder svg, PlanoCorteItem item, double x0, double y0,
                                          double larguraPx, double alturaPx, double escala) {
        double raio = Math.min(larguraPx, alturaPx) / 2;
        double cx = x0 + larguraPx / 2;
        double cy = y0 + alturaPx / 2;
        svg.append(String.format(Locale.US,
                "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"#eff6ff\" stroke=\"#1e293b\" stroke-width=\"2\"/>",
                cx, cy, raio));
    }

    private void desenharBisoteRedondo(StringBuilder svg, PlanoCorteItem item, double x0, double y0,
                                        double larguraPx, double alturaPx, double escala) {
        if (item.getEspessuraBisoteMm() == null || item.getEspessuraBisoteMm().signum() <= 0) {
            return;
        }
        double raioExterno = Math.min(larguraPx, alturaPx) / 2;
        double faixaPx = Math.max(item.getEspessuraBisoteMm().doubleValue() * escala, 2);
        double raioInterno = Math.max(raioExterno - faixaPx, 0);
        double cx = x0 + larguraPx / 2;
        double cy = y0 + alturaPx / 2;

        svg.append(String.format(Locale.US,
                "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"none\" stroke=\"#0ea5e9\" stroke-width=\"1.3\" stroke-dasharray=\"5 3\"/>",
                cx, cy, raioInterno));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" fill=\"#0369a1\" text-anchor=\"middle\">Bisô %s mm (toda a circunferência)</text>",
                cx, cy - raioInterno - 6, FONTE, formatarNumero(item.getEspessuraBisoteMm())));
    }


    private void desenharContornoPeca(StringBuilder svg, PlanoCorteItem item, double x0, double y0,
                                       double larguraPx, double alturaPx, double escala) {
        boolean acabamentoDeCanto = item.getTipoBorda() == TipoBorda.CANTO_MOEDA
                || item.getTipoBorda() == TipoBorda.CANTO_GARRAFA;
        if (!acabamentoDeCanto || !item.isTemCantoArredondado()) {
            svg.append(String.format(Locale.US,
                    "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"#eff6ff\" stroke=\"#1e293b\" stroke-width=\"2\"/>",
                    x0, y0, larguraPx, alturaPx));
            return;
        }


        double raioMm = 25;
        double raioMaximoPx = Math.min(larguraPx, alturaPx) / 2.2;
        double raio = Math.max(Math.min(raioMm * escala, raioMaximoPx), 4);

        double rTL = item.isCantoMoedaSuperiorEsquerdo() ? raio : 0;
        double rTR = item.isCantoMoedaSuperiorDireito() ? raio : 0;
        double rBR = item.isCantoMoedaInferiorDireito() ? raio : 0;
        double rBL = item.isCantoMoedaInferiorEsquerdo() ? raio : 0;

        double xw = x0 + larguraPx;
        double yh = y0 + alturaPx;

        StringBuilder d = new StringBuilder();
        d.append(String.format(Locale.US, "M %.1f %.1f ", x0 + rTL, y0));
        d.append(String.format(Locale.US, "L %.1f %.1f ", xw - rTR, y0));
        if (rTR > 0) {
            d.append(String.format(Locale.US, "A %.1f %.1f 0 0 1 %.1f %.1f ", rTR, rTR, xw, y0 + rTR));
        }
        d.append(String.format(Locale.US, "L %.1f %.1f ", xw, yh - rBR));
        if (rBR > 0) {
            d.append(String.format(Locale.US, "A %.1f %.1f 0 0 1 %.1f %.1f ", rBR, rBR, xw - rBR, yh));
        }
        d.append(String.format(Locale.US, "L %.1f %.1f ", x0 + rBL, yh));
        if (rBL > 0) {
            d.append(String.format(Locale.US, "A %.1f %.1f 0 0 1 %.1f %.1f ", rBL, rBL, x0, yh - rBL));
        }
        d.append(String.format(Locale.US, "L %.1f %.1f ", x0, y0 + rTL));
        if (rTL > 0) {
            d.append(String.format(Locale.US, "A %.1f %.1f 0 0 1 %.1f %.1f ", rTL, rTL, x0 + rTL, y0));
        }
        d.append("Z");

        svg.append(String.format(Locale.US,
                "<path d=\"%s\" fill=\"#eff6ff\" stroke=\"#1e293b\" stroke-width=\"2\"/>", d));



        String rotuloCanto = item.getTipoBorda().getDescricao().toUpperCase(Locale.ROOT);
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"11\" font-weight=\"800\" fill=\"#7c3aed\" letter-spacing=\"0.3\">%s</text>",
                x0 + 10, y0 + 18, FONTE, escapeXml(rotuloCanto)));
    }

    private String defsSetas() {
        return "<defs>"
                + "<marker id=\"seta-fim\" viewBox=\"0 0 10 10\" refX=\"5\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto\">"
                + "<path d=\"M0,0 L10,5 L0,10 z\" fill=\"#94a3b8\"/>"
                + "</marker>"
                + "<marker id=\"seta-inicio\" viewBox=\"0 0 10 10\" refX=\"5\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto\">"
                + "<path d=\"M10,0 L0,5 L10,10 z\" fill=\"#94a3b8\"/>"
                + "</marker>"
                + "<marker id=\"anotacao-seta-fim\" viewBox=\"0 0 10 10\" refX=\"8\" refY=\"5\" markerWidth=\"7\" markerHeight=\"7\" orient=\"auto\">"
                + "<path d=\"M0,0 L10,5 L0,10 z\" fill=\"#2563eb\"/>"
                + "</marker>"
                + "</defs>";
    }


    private void desenharAnotacoes(StringBuilder svg, List<Anotacao> anotacoes, double x0, double y0, double escala) {
        if (anotacoes == null) {
            return;
        }
        for (Anotacao anotacao : anotacoes) {
            if (anotacao == null || anotacao.getTipo() == null || anotacao.getX1Mm() == null || anotacao.getY1Mm() == null) {
                continue;
            }
            double x1 = x0 + anotacao.getX1Mm().doubleValue() * escala;
            double y1 = y0 + anotacao.getY1Mm().doubleValue() * escala;

            switch (anotacao.getTipo()) {
                case TEXTO -> {
                    if (anotacao.getTexto() != null && !anotacao.getTexto().isBlank()) {
                        svg.append(String.format(Locale.US,
                                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"12\" font-weight=\"700\" fill=\"#2563eb\">%s</text>",
                                x1, y1, FONTE, escapeXml(anotacao.getTexto())));
                    }
                }
                case LINHA, SETA -> {
                    if (anotacao.getX2Mm() == null || anotacao.getY2Mm() == null) {
                        continue;
                    }
                    double x2 = x0 + anotacao.getX2Mm().doubleValue() * escala;
                    double y2 = y0 + anotacao.getY2Mm().doubleValue() * escala;
                    String marcador = anotacao.getTipo() == TipoAnotacao.SETA
                            ? " marker-end=\"url(#anotacao-seta-fim)\""
                            : "";
                    svg.append(String.format(Locale.US,
                            "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#2563eb\" stroke-width=\"1.8\"%s/>",
                            x1, y1, x2, y2, marcador));
                    if (anotacao.getTexto() != null && !anotacao.getTexto().isBlank()) {
                        svg.append(String.format(Locale.US,
                                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"11\" font-weight=\"600\" fill=\"#2563eb\" text-anchor=\"middle\">%s</text>",
                                (x1 + x2) / 2, Math.min(y1, y2) - 6, FONTE, escapeXml(anotacao.getTexto())));
                    }
                }
            }
        }
    }

    private void desenharBisote(StringBuilder svg, PlanoCorteItem item, double x0, double y0, double larguraPx, double alturaPx, double escala) {
        if (item.getEspessuraBisoteMm() == null || item.getEspessuraBisoteMm().signum() <= 0) {
            return;
        }
        double faixaPx = Math.max(item.getEspessuraBisoteMm().doubleValue() * escala, 2);
        svg.append(String.format(Locale.US,
                "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"none\" stroke=\"#0ea5e9\" stroke-width=\"1.3\" stroke-dasharray=\"5 3\"/>",
                x0 + faixaPx, y0 + faixaPx, Math.max(larguraPx - 2 * faixaPx, 0), Math.max(alturaPx - 2 * faixaPx, 0)));

        double cotaY = y0 + Math.min(faixaPx, alturaPx / 4) + 15;
        svg.append(linhaCota(x0, cotaY, x0 + faixaPx, cotaY));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" fill=\"#0369a1\" text-anchor=\"start\">Bisô %s mm (todas as laterais)</text>",
                x0 + faixaPx + 5, cotaY + 4, FONTE, formatarNumero(item.getEspessuraBisoteMm())));
    }

    private void desenharFuracoes(StringBuilder svg, List<Furacao> furacoes, double x0, double y0, double escala) {
        List<Furacao> restantes = new ArrayList<>(furacoes);
        List<Furacao[]> paresPuxador = new ArrayList<>();

        for (int i = 0; i < restantes.size(); i++) {
            Furacao a = restantes.get(i);
            if (a == null || a.getTipo() != TipoFuracao.PUXADOR || a.getPosicaoXMm() == null) {
                continue;
            }
            for (int j = i + 1; j < restantes.size(); j++) {
                Furacao b = restantes.get(j);
                if (b == null || b.getTipo() != TipoFuracao.PUXADOR || b.getPosicaoXMm() == null) {
                    continue;
                }
                if (a.getPosicaoXMm().subtract(b.getPosicaoXMm()).abs().doubleValue() <= TOLERANCIA_PAR_MM) {
                    paresPuxador.add(new Furacao[]{a, b});
                    restantes.set(i, null);
                    restantes.set(j, null);
                    break;
                }
            }
        }

        for (Furacao[] par : paresPuxador) {
            desenharPuxadorH(svg, par[0], par[1], x0, y0, escala);
        }
        for (Furacao furacao : restantes) {
            if (furacao == null || furacao.getPosicaoXMm() == null || furacao.getPosicaoYMm() == null) {
                continue;
            }
            desenharFuracaoSimples(svg, furacao, x0, y0, escala);
        }
    }

    private void desenharFuracaoSimples(StringBuilder svg, Furacao furacao, double x0, double y0, double escala) {
        double cx = x0 + furacao.getPosicaoXMm().doubleValue() * escala;
        double cy = y0 + furacao.getPosicaoYMm().doubleValue() * escala;
        double raio = furacao.getDiametroMm() != null ? Math.max(furacao.getDiametroMm().doubleValue() * escala / 2, 4) : 5;

        svg.append(String.format(Locale.US,
                "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"#ffffff\" stroke=\"#0f172a\" stroke-width=\"1.6\"/>",
                cx, cy, raio));

        String rotuloPermitido = rotuloFuracaoPermitido(furacao);
        if (rotuloPermitido != null) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" font-weight=\"700\" fill=\"#0f172a\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"middle\">%s</text>",
                    cx, cy - raio - 7, FONTE, rotuloPermitido));
        }


        if (furacao.getDiametroMm() != null && !ocultarDiametroNoCroqui(furacao)) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#94a3b8\" text-anchor=\"middle\">Ø%smm</text>",
                    cx, cy + raio + 12, FONTE, formatarNumero(furacao.getDiametroMm())));
        }
    }

    private void desenharPuxadorH(StringBuilder svg, Furacao a, Furacao b, double x0, double y0, double escala) {
        double cx = x0 + a.getPosicaoXMm().doubleValue() * escala;
        double cySuperior = y0 + Math.min(a.getPosicaoYMm().doubleValue(), b.getPosicaoYMm().doubleValue()) * escala;
        double cyInferior = y0 + Math.max(a.getPosicaoYMm().doubleValue(), b.getPosicaoYMm().doubleValue()) * escala;

        svg.append(String.format(Locale.US,
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#334155\" stroke-width=\"4\" stroke-linecap=\"round\"/>",
                cx, cySuperior, cx, cyInferior));
        svg.append(String.format(Locale.US,
                "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#ffffff\" stroke=\"#0f172a\" stroke-width=\"1.6\"/>", cx, cySuperior));
        svg.append(String.format(Locale.US,
                "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"5\" fill=\"#ffffff\" stroke=\"#0f172a\" stroke-width=\"1.6\"/>", cx, cyInferior));

        BigDecimal comprimentoMm = a.getPosicaoYMm().subtract(b.getPosicaoYMm()).abs();
        svg.append(String.format(Locale.US,
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-width=\"1.3\" marker-start=\"url(#seta-inicio)\" marker-end=\"url(#seta-fim)\"/>",
                cx + 16, cySuperior, cx + 16, cyInferior));
        svg.append(String.format(Locale.US,
                "<text x=\"0\" y=\"0\" font-family=\"%s\" font-size=\"10\" fill=\"#475569\" text-anchor=\"middle\" transform=\"translate(%.1f %.1f) rotate(-90)\">%s mm</text>",
                FONTE, cx + 30, (cySuperior + cyInferior) / 2, formatarNumero(comprimentoMm)));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" font-weight=\"700\" fill=\"#0f172a\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"middle\">PUXADOR H</text>",
                cx, cySuperior - 27, FONTE));
        if (a.getDiametroMm() != null) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" fill=\"#475569\" text-anchor=\"middle\">&#216;%smm</text>",
                    cx, cySuperior - 13, FONTE, formatarNumero(a.getDiametroMm())));
        }
    }

    private String rotuloFuracaoPermitido(Furacao furacao) {
        if (furacao.getTipo() == null) {
            return null;
        }
        String descricao = furacao.getDescricao();
        return switch (furacao.getTipo()) {
            case ROLDANA -> "ROLDANA";
            case PUXADOR -> descricao != null && descricao.toLowerCase(Locale.ROOT).contains("simples")
                    ? "PUXADOR SIMPLES" : null;
            case FECHADURA -> descricao != null && !descricao.isBlank() ? "FECHADURA" : null;
            case BATE_FECHA -> descricao != null && !descricao.isBlank() ? "BATE E FECHA" : null;
            default -> null;
        };
    }

    private boolean ocultarDiametroNoCroqui(Furacao furacao) {
        return furacao.getTipo() == TipoFuracao.ROLDANA
                || furacao.getTipo() == TipoFuracao.FECHADURA
                || furacao.getTipo() == TipoFuracao.BATE_FECHA
                || (furacao.getTipo() == TipoFuracao.PUXADOR
                    && furacao.getDescricao() != null
                    && furacao.getDescricao().toLowerCase(Locale.ROOT).contains("simples"));
    }

    private void desenharElemento(StringBuilder svg, ElementoTecnico elemento, GeometriaElemento geometria,
                                  LayoutCotaElemento layout, double escala) {
        if (geometria == null) {
            return;
        }
        double cx = geometria.posicaoX();
        double cy = geometria.posicaoY();
        String cor = "#b45309";
        String medida = medidaElemento(elemento);


        boolean recorteComAncoragem = elemento.getTipo() == TipoElemento.RECORTE
                && elemento.getAncoragem() == TipoAncoragem.CANTO;
        boolean ancoraEsquerda = recorteComAncoragem && elemento.getReferenciaHorizontal() == ReferenciaHorizontal.ESQUERDA;
        boolean ancoraDireita = recorteComAncoragem && elemento.getReferenciaHorizontal() == ReferenciaHorizontal.DIREITA;
        boolean ancoraSuperior = recorteComAncoragem && elemento.getReferenciaVertical() == ReferenciaVertical.SUPERIOR;
        boolean ancoraInferior = recorteComAncoragem && elemento.getReferenciaVertical() == ReferenciaVertical.INFERIOR;
        boolean ancoraHorizontal = ancoraEsquerda || ancoraDireita;
        boolean ancoraVertical = ancoraSuperior || ancoraInferior;


        double centroX = cx;
        double centroY = cy;
        double labelCx = cx;
        double topoY = cy;
        double meioExtensaoV = 10;
        double meioExtensaoH = 10;

        switch (elemento.getTipo()) {
            case FURO -> {
                double r = elemento.getDiametroMm() != null ? Math.max(elemento.getDiametroMm().doubleValue() * escala / 2, 4) : 5;
                svg.append(String.format(Locale.US,
                        "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"#fffbeb\" stroke=\"%s\" stroke-width=\"1.6\"/>", cx, cy, r, cor));
                topoY = cy - r;
                meioExtensaoV = r;
                meioExtensaoH = r;
            }
            case RASGO -> {
                double compPx = Math.max((elemento.getComprimentoMm() != null ? elemento.getComprimentoMm().doubleValue() : 20) * escala, 14);
                double largPx = Math.max((elemento.getLarguraMm() != null ? elemento.getLarguraMm().doubleValue() : 6) * escala, 6);
                svg.append(String.format(Locale.US,
                        "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"%.1f\" fill=\"#fffbeb\" stroke=\"%s\" stroke-width=\"1.6\"/>",
                        cx - compPx / 2, cy - largPx / 2, compPx, largPx, largPx / 2, cor));
                topoY = cy - largPx / 2;
                meioExtensaoV = largPx / 2;
                meioExtensaoH = compPx / 2;
            }
            case RECORTE -> {
                double largPx = Math.max((elemento.getLarguraMm() != null ? elemento.getLarguraMm().doubleValue() : 20) * escala, 10);
                double altPx = Math.max((elemento.getAlturaMm() != null ? elemento.getAlturaMm().doubleValue() : 20) * escala, 10);
                double rectX = ancoraHorizontal ? (ancoraDireita ? cx - largPx : cx) : cx - largPx / 2;
                double rectY = ancoraVertical ? (ancoraInferior ? cy - altPx : cy) : cy - altPx / 2;
                centroX = rectX + largPx / 2;
                centroY = rectY + altPx / 2;
                meioExtensaoH = largPx / 2;
                meioExtensaoV = altPx / 2;
                svg.append(String.format(Locale.US,
                        "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"#ffffff\" stroke=\"#000000\" stroke-width=\"1.6\"/>",
                        rectX, rectY, largPx, altPx));
                labelCx = centroX;
                topoY = rectY;
            }
            case CHANFRO -> {
                double comp = Math.max((elemento.getComprimentoMm() != null ? elemento.getComprimentoMm().doubleValue() : 15) * escala, 8);
                svg.append(String.format(Locale.US,
                        "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"2.4\"/>",
                        cx - comp / 2, cy + comp / 2, cx + comp / 2, cy - comp / 2, cor));
                topoY = cy - comp / 2;
                meioExtensaoV = comp / 2;
                meioExtensaoH = comp / 2;
            }
            case BOLEADO -> {
                double raio = Math.max((elemento.getRaioMm() != null ? elemento.getRaioMm().doubleValue() : 10) * escala, 7);
                svg.append(String.format(Locale.US,
                        "<path d=\"M %.1f %.1f A %.1f %.1f 0 0 1 %.1f %.1f\" fill=\"none\" stroke=\"%s\" stroke-width=\"2.4\"/>",
                        cx - raio, cy, raio, raio, cx, cy - raio, cor));
                topoY = cy - raio;
                meioExtensaoV = raio;
                meioExtensaoH = raio;
            }
        }

        if (elemento.getTipo() == TipoElemento.FURO
                || elemento.getTipo() == TipoElemento.RECORTE
                || elemento.getTipo() == TipoElemento.RASGO) {
            desenharMarcaCentro(svg, centroX, centroY);
        }

        if (medida != null) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" font-weight=\"600\" fill=\"#92400e\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"middle\">%s</text>",
                    labelCx, layout.labelY(), FONTE, escapeXml(medida)));
        }

        desenharCotasElemento(svg, geometria, layout, escala);
    }


    private GeometriaElemento calcularGeometriaElemento(ElementoTecnico elemento, double x0, double y0,
                                                         double escala) {
        if (elemento == null || elemento.getPosicaoXMm() == null || elemento.getPosicaoYMm() == null
                || elemento.getTipo() == null) {
            return null;
        }
        double posicaoX = x0 + elemento.getPosicaoXMm().doubleValue() * escala;
        double posicaoY = y0 + elemento.getPosicaoYMm().doubleValue() * escala;
        double centroX = posicaoX;
        double centroY = posicaoY;
        double meiaLargura = 10;
        double meiaAltura = 10;
        double labelX = posicaoX;
        double topoY = posicaoY;

        switch (elemento.getTipo()) {
            case FURO -> {
                double raio = elemento.getDiametroMm() != null
                        ? Math.max(elemento.getDiametroMm().doubleValue() * escala / 2, 4) : 5;
                meiaLargura = raio;
                meiaAltura = raio;
                topoY = centroY - raio;
            }
            case RASGO -> {
                meiaLargura = Math.max(
                        (elemento.getComprimentoMm() != null ? elemento.getComprimentoMm().doubleValue() : 20)
                                * escala, 14) / 2;
                meiaAltura = Math.max(
                        (elemento.getLarguraMm() != null ? elemento.getLarguraMm().doubleValue() : 6)
                                * escala, 6) / 2;
                topoY = centroY - meiaAltura;
            }
            case RECORTE -> {
                meiaLargura = Math.max(
                        (elemento.getLarguraMm() != null ? elemento.getLarguraMm().doubleValue() : 20)
                                * escala, 10) / 2;
                meiaAltura = Math.max(
                        (elemento.getAlturaMm() != null ? elemento.getAlturaMm().doubleValue() : 20)
                                * escala, 10) / 2;
                boolean ancorado = elemento.getAncoragem() == TipoAncoragem.CANTO;
                if (ancorado && elemento.getReferenciaHorizontal() == ReferenciaHorizontal.ESQUERDA) {
                    centroX += meiaLargura;
                } else if (ancorado && elemento.getReferenciaHorizontal() == ReferenciaHorizontal.DIREITA) {
                    centroX -= meiaLargura;
                }
                if (ancorado && elemento.getReferenciaVertical() == ReferenciaVertical.SUPERIOR) {
                    centroY += meiaAltura;
                } else if (ancorado && elemento.getReferenciaVertical() == ReferenciaVertical.INFERIOR) {
                    centroY -= meiaAltura;
                }
                labelX = centroX;
                topoY = centroY - meiaAltura;
            }
            case CHANFRO -> {
                double metade = Math.max(
                        (elemento.getComprimentoMm() != null ? elemento.getComprimentoMm().doubleValue() : 15)
                                * escala, 8) / 2;
                meiaLargura = metade;
                meiaAltura = metade;
                topoY = centroY - metade;
            }
            case BOLEADO -> {
                double raio = Math.max(
                        (elemento.getRaioMm() != null ? elemento.getRaioMm().doubleValue() : 10)
                                * escala, 7);
                meiaLargura = raio;
                meiaAltura = raio;
                topoY = centroY - raio;
            }
        }
        return new GeometriaElemento(posicaoX, posicaoY, centroX, centroY,
                meiaLargura, meiaAltura, labelX, topoY);
    }

    private List<LayoutCotaElemento> planejarCotasElementos(List<ElementoTecnico> elementos,
                                                            List<GeometriaElemento> geometrias,
                                                            double x0, double y0, double larguraPecaPx,
                                                            double alturaPecaPx, double escala) {
        List<LayoutCotaElemento> layouts = new ArrayList<>();
        List<FaixaCota> horizontais = new ArrayList<>();
        List<FaixaCota> verticais = new ArrayList<>();
        List<FaixaCota> rotulos = new ArrayList<>();
        for (int indice = 0; indice < elementos.size(); indice++) {
            ElementoTecnico elemento = elementos.get(indice);
            GeometriaElemento geometria = geometrias.get(indice);
            if (geometria == null) {
                layouts.add(new LayoutCotaElemento(null, null, null, null, false, y0));
                continue;
            }
            GeometriaElemento referencia = null;
            Integer referenciaIndice = elemento.getReferenciaElementoIndice();
            if (referenciaIndice != null && referenciaIndice >= 0 && referenciaIndice < geometrias.size()) {
                referencia = geometrias.get(referenciaIndice);
            }
            Double origemX;
            Double origemY;
            if (referencia != null) {
                origemX = referencia.centroX();
                origemY = referencia.centroY();
            } else {
                origemX = origemHorizontal(elemento.getReferenciaHorizontal(), x0, larguraPecaPx);
                origemY = origemVertical(elemento.getReferenciaVertical(), y0, alturaPecaPx);
            }

            Double linhaH = null;
            if (elemento.getReferenciaHorizontal() != null && origemX != null
                    && Math.abs(origemX - geometria.centroX()) > 3) {
                linhaH = escolherLinhaLivre(true, geometria, origemX, geometrias, horizontais,
                        y0, alturaPecaPx);
                horizontais.add(new FaixaCota(Math.min(origemX, geometria.centroX()),
                        Math.max(origemX, geometria.centroX()), linhaH));
            }
            Double linhaV = null;
            if (elemento.getReferenciaVertical() != null && origemY != null
                    && Math.abs(origemY - geometria.centroY()) > 3) {
                linhaV = escolherLinhaLivre(false, geometria, origemY, geometrias, verticais,
                        x0, larguraPecaPx);
                verticais.add(new FaixaCota(Math.min(origemY, geometria.centroY()),
                        Math.max(origemY, geometria.centroY()), linhaV));
            }
            String medida = medidaElemento(elemento);
            double meiaLarguraRotulo = Math.max(16, medida != null ? medida.length() * 3.1 : 16);
            double labelY = geometria.topoY() - 9;
            while (interceptaFaixa(geometria.labelX() - meiaLarguraRotulo,
                    geometria.labelX() + meiaLarguraRotulo, labelY, rotulos, 12)
                    && labelY > y0 + 12) {
                labelY -= 13;
            }
            labelY = limitar(labelY, y0 + 10, y0 + alturaPecaPx - 6);
            rotulos.add(new FaixaCota(geometria.labelX() - meiaLarguraRotulo,
                    geometria.labelX() + meiaLarguraRotulo, labelY));
            layouts.add(new LayoutCotaElemento(origemX, origemY, linhaH, linhaV,
                    referencia != null, labelY));
        }
        return layouts;
    }

    private Double origemHorizontal(ReferenciaHorizontal referencia, double inicio, double tamanho) {
        if (referencia == null) return null;
        return switch (referencia) {
            case ESQUERDA -> inicio;
            case DIREITA -> inicio + tamanho;
            case CENTRO -> inicio + tamanho / 2;
        };
    }

    private Double origemVertical(ReferenciaVertical referencia, double inicio, double tamanho) {
        if (referencia == null) return null;
        return switch (referencia) {
            case SUPERIOR -> inicio;
            case INFERIOR -> inicio + tamanho;
            case CENTRO -> inicio + tamanho / 2;
        };
    }

    private double escolherLinhaLivre(boolean horizontal, GeometriaElemento alvo, double origem,
                                      List<GeometriaElemento> geometrias, List<FaixaCota> ocupadas,
                                      double inicioPeca, double tamanhoPeca) {
        double centro = horizontal ? alvo.centroY() : alvo.centroX();
        double meio = horizontal ? alvo.meiaAltura() : alvo.meiaLargura();
        double intervaloFim = horizontal ? alvo.centroX() : alvo.centroY();
        double minimo = Math.min(origem, intervaloFim);
        double maximo = Math.max(origem, intervaloFim);
        double direcaoPreferida = centro < inicioPeca + tamanhoPeca / 2 ? 1 : -1;
        double passo = horizontal ? 18 : 24;
        double base = meio + passo;

        for (int nivel = 0; nivel < 8; nivel++) {
            for (int lado = 0; lado < 2; lado++) {
                double direcao = lado == 0 ? direcaoPreferida : -direcaoPreferida;
                double candidato = centro + direcao * (base + nivel * passo);
                if (candidato < inicioPeca + 12 || candidato > inicioPeca + tamanhoPeca - 12) continue;
                if (!interceptaElemento(horizontal, minimo, maximo, candidato, geometrias)
                        && !interceptaFaixa(minimo, maximo, candidato, ocupadas, horizontal ? 15 : 22)) {
                    return candidato;
                }
            }
        }
        return limitar(centro + direcaoPreferida * base,
                inicioPeca + 12, inicioPeca + tamanhoPeca - 12);
    }

    private boolean interceptaElemento(boolean horizontal, double minimo, double maximo,
                                       double coordenada, List<GeometriaElemento> geometrias) {
        for (GeometriaElemento geometria : geometrias) {
            if (geometria == null) continue;
            double inicio = horizontal ? geometria.centroX() - geometria.meiaLargura() - 8
                    : geometria.centroY() - geometria.meiaAltura() - 8;
            double fim = horizontal ? geometria.centroX() + geometria.meiaLargura() + 8
                    : geometria.centroY() + geometria.meiaAltura() + 8;
            double centro = horizontal ? geometria.centroY() : geometria.centroX();
            double meio = horizontal ? geometria.meiaAltura() : geometria.meiaLargura();
            if (intervalosSobrepostos(minimo, maximo, inicio, fim)
                    && Math.abs(coordenada - centro) <= meio + 10) return true;
        }
        return false;
    }

    private boolean interceptaFaixa(double minimo, double maximo, double coordenada,
                                     List<FaixaCota> faixas, double afastamento) {
        return faixas.stream().anyMatch(faixa -> intervalosSobrepostos(
                minimo, maximo, faixa.inicio(), faixa.fim())
                && Math.abs(coordenada - faixa.coordenada()) < afastamento);
    }

    private boolean intervalosSobrepostos(double inicioA, double fimA, double inicioB, double fimB) {
        return inicioA <= fimB && inicioB <= fimA;
    }

    private double limitar(double valor, double minimo, double maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    private record GeometriaElemento(double posicaoX, double posicaoY, double centroX, double centroY,
                                     double meiaLargura, double meiaAltura, double labelX, double topoY) {
    }

    private record LayoutCotaElemento(Double origemX, Double origemY, Double yLinhaHorizontal,
                                      Double xLinhaVertical, boolean origemElemento, double labelY) {
    }

    private record FaixaCota(double inicio, double fim, double coordenada) {
    }

    private void desenharCotasElemento(StringBuilder svg, GeometriaElemento geometria,
                                        LayoutCotaElemento layout, double escala) {
        if (layout.yLinhaHorizontal() != null && layout.origemX() != null) {
            double yLinha = layout.yLinhaHorizontal();
            svg.append(linhaCotaElemento(layout.origemX(), yLinha,
                    geometria.centroX(), yLinha, "horizontal"));
            svg.append(linhaAuxiliarCota(geometria.centroX(), geometria.centroY(),
                    geometria.centroX(), yLinha));
            if (layout.origemElemento()) {
                svg.append(linhaAuxiliarCota(layout.origemX(), layout.origemY(),
                        layout.origemX(), yLinha));
            }
            double labelX = (layout.origemX() + geometria.centroX()) / 2;
            double distanciaMm = Math.abs(layout.origemX() - geometria.centroX()) / escala;
            svg.append(String.format(Locale.US,
                    "<text class=\"rotulo-cota-elemento\" x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#92400e\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"middle\">%s mm</text>",
                    labelX, yLinha - 7, FONTE, formatarNumeroMm(distanciaMm)));
        }

        if (layout.xLinhaVertical() != null && layout.origemY() != null) {
            double xLinha = layout.xLinhaVertical();
            svg.append(linhaCotaElemento(xLinha, layout.origemY(),
                    xLinha, geometria.centroY(), "vertical"));
            svg.append(linhaAuxiliarCota(geometria.centroX(), geometria.centroY(),
                    xLinha, geometria.centroY()));
            if (layout.origemElemento()) {
                svg.append(linhaAuxiliarCota(layout.origemX(), layout.origemY(),
                        xLinha, layout.origemY()));
            }
            double labelY = (layout.origemY() + geometria.centroY()) / 2;
            double distanciaMm = Math.abs(layout.origemY() - geometria.centroY()) / escala;
            svg.append(String.format(Locale.US,
                    "<text class=\"rotulo-cota-elemento\" x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#92400e\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"start\">%s mm</text>",
                    xLinha + 10, labelY + 3, FONTE, formatarNumeroMm(distanciaMm)));
        }
    }

    private void desenharMarcaCentro(StringBuilder svg, double centroX, double centroY) {
        svg.append(String.format(Locale.US,
                "<path class=\"marca-centro-elemento\" d=\"M %.1f %.1f H %.1f M %.1f %.1f V %.1f\" fill=\"none\" stroke=\"#b45309\" stroke-width=\"0.9\"/>",
                centroX - 3, centroY, centroX + 3, centroX, centroY - 3, centroY + 3));
    }

    private String linhaCotaElemento(double x1, double y1, double x2, double y2, String orientacao) {
        return String.format(Locale.US,
                "<line class=\"cota-elemento cota-elemento-%s\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-width=\"1.3\" marker-start=\"url(#seta-inicio)\" marker-end=\"url(#seta-fim)\"/>",
                orientacao, x1, y1, x2, y2);
    }

    private String linhaAuxiliarCota(double x1, double y1, double x2, double y2) {
        return String.format(Locale.US,
                "<line class=\"linha-auxiliar-cota\" x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#d97706\" stroke-width=\"0.9\" stroke-dasharray=\"3 2\"/>",
                x1, y1, x2, y2);
    }

    private void desenharCotasElemento(StringBuilder svg, double centroX, double centroY,
                                        double x0, double y0, double larguraPecaPx, double alturaPecaPx,
                                        double meioExtensaoH, double meioExtensaoV,
                                        ReferenciaHorizontal refH, ReferenciaVertical refV, double escala,
                                        Double referenciaXMm, Double referenciaYMm, int indice) {
        double faixa = faixaCota(indice);
        if (refH != null) {
            double xOrigem = referenciaXMm != null
                    ? x0 + referenciaXMm * escala
                    : switch (refH) {
                        case ESQUERDA -> x0;
                        case DIREITA -> x0 + larguraPecaPx;
                        case CENTRO -> x0 + larguraPecaPx / 2;
                    };
            double yLinha = centroY + faixa;
            if (Math.abs(xOrigem - centroX) > 3) {
                svg.append(linhaCota(xOrigem, yLinha, centroX, yLinha));
                double labelX = (xOrigem + centroX) / 2;
                double distanciaMm = Math.abs(xOrigem - centroX) / escala;
                svg.append(String.format(Locale.US,
                        "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#92400e\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"middle\">%s mm</text>",
                        labelX, yLinha - 7, FONTE, formatarNumeroMm(distanciaMm)));
            }
        }

        if (refV != null) {
            double yOrigem = referenciaYMm != null
                    ? y0 + referenciaYMm * escala
                    : switch (refV) {
                        case SUPERIOR -> y0;
                        case INFERIOR -> y0 + alturaPecaPx;
                        case CENTRO -> y0 + alturaPecaPx / 2;
                    };
            double xLinha = centroX + faixa * 1.8;
            if (Math.abs(yOrigem - centroY) > 3) {
                svg.append(linhaCota(xLinha, yOrigem, xLinha, centroY));
                double labelY = (yOrigem + centroY) / 2;
                double distanciaMm = Math.abs(yOrigem - centroY) / escala;
                svg.append(String.format(Locale.US,
                        "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#92400e\" stroke=\"#ffffff\" stroke-width=\"3\" paint-order=\"stroke\" text-anchor=\"start\">%s mm</text>",
                        xLinha + 10, labelY + 3, FONTE, formatarNumeroMm(distanciaMm)));
            }
        }
    }

    private double faixaCota(int indice) {
        if (indice == 0) {
            return 0;
        }
        int nivel = (indice + 1) / 2;
        return (indice % 2 == 1 ? 1 : -1) * nivel * 18.0;
    }

    private String medidaElemento(ElementoTecnico elemento) {
        if (elemento.getRotuloCroqui() != null) {
            return elemento.getRotuloCroqui().isBlank() ? null : elemento.getRotuloCroqui();
        }
        return switch (elemento.getTipo()) {
            case FURO -> "Ø" + (elemento.getDiametroMm() != null ? formatarNumero(elemento.getDiametroMm()) : "?") + "mm";
            case RECORTE, RASGO ->
                    (elemento.getLarguraMm() != null ? formatarNumero(elemento.getLarguraMm()) : "?") + "x"
                            + (elemento.getAlturaMm() != null ? formatarNumero(elemento.getAlturaMm()) : "?") + "mm";
            case CHANFRO -> (elemento.getComprimentoMm() != null ? formatarNumero(elemento.getComprimentoMm()) : "?") + "mm";
            case BOLEADO -> "R" + (elemento.getRaioMm() != null ? formatarNumero(elemento.getRaioMm()) : "?") + "mm";
        };
    }

    private String rotuloElemento(ElementoTecnico elemento) {
        if (elemento.getNome() != null && !elemento.getNome().isBlank()) {
            return elemento.getNome();
        }
        if (elemento.getFerragemNomeSnapshot() != null) {
            return elemento.getFerragemNomeSnapshot();
        }
        return elemento.getTipo() != null ? elemento.getTipo().getDescricao() : null;
    }

    private String linhaCota(double x1, double y1, double x2, double y2) {
        return String.format(Locale.US,
                "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#94a3b8\" stroke-width=\"1.3\" marker-start=\"url(#seta-inicio)\" marker-end=\"url(#seta-fim)\"/>",
                x1, y1, x2, y2);
    }

    private String formatarNumero(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        return valor.stripTrailingZeros().toPlainString();
    }


    private String formatarNumeroMm(double valorMm) {
        return formatarNumero(BigDecimal.valueOf(valorMm).setScale(1, java.math.RoundingMode.HALF_UP));
    }

    private String resumo(String texto, int max) {
        if (texto.length() <= max) {
            return texto;
        }
        return texto.substring(0, max - 1) + "…";
    }

    private String escapeXml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
