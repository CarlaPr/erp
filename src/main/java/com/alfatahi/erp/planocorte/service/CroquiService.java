package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.entity.ElementoTecnico;
import com.alfatahi.erp.planocorte.entity.Furacao;
import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import com.alfatahi.erp.planocorte.entity.TipoFolha;
import com.alfatahi.erp.planocorte.entity.TipoFuracao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CroquiService {

    private static final double MAX_LADO_PX = 380;
    private static final double MARGEM_PX = 60;
    private static final double TITULO_PX = 44;
    private static final double TOLERANCIA_PAR_MM = 5;

    private static final String FONTE = "Helvetica, Arial, sans-serif";

    public String gerarSvg(PlanoCorteItem item) {
        double larguraMm = item.getLarguraFinalMm().doubleValue();
        double alturaMm = item.getAlturaFinalMm().doubleValue();

        double maiorLado = Math.max(larguraMm, alturaMm);
        double escala = maiorLado > 0 ? MAX_LADO_PX / maiorLado : 1;

        double larguraPx = Math.max(larguraMm * escala, 20);
        double alturaPx = Math.max(alturaMm * escala, 20);

        double x0 = MARGEM_PX;
        double y0 = MARGEM_PX + TITULO_PX;
        double svgW = larguraPx + MARGEM_PX * 2;
        double svgH = alturaPx + MARGEM_PX * 2 + TITULO_PX;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.0f\" height=\"%.0f\" viewBox=\"0 0 %.0f %.0f\" preserveAspectRatio=\"xMidYMid meet\">",
                svgW, svgH, svgW, svgH));
        svg.append(defsSetas());

        svg.append(String.format(Locale.US,
                "<text x=\"%.0f\" y=\"20\" font-family=\"%s\" font-size=\"16\" font-weight=\"700\" fill=\"#0f172a\" letter-spacing=\"0.4\">%s</text>",
                MARGEM_PX, FONTE, escapeXml(item.getCodigoPeca())));

        String titulo = item.getVidroNomeSnapshot() + " · " + formatarNumero(item.getEspessuraSnapshot()) + "mm";
        if (item.getTipoFolha() != null && item.getTipoFolha() != TipoFolha.UNICA) {
            titulo += " · " + item.getTipoFolha().getDescricao();
        }
        svg.append(String.format(Locale.US,
                "<text x=\"%.0f\" y=\"39\" font-family=\"%s\" font-size=\"13\" fill=\"#64748b\">%s</text>",
                MARGEM_PX, FONTE, escapeXml(titulo)));

        svg.append(String.format(Locale.US,
                "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"#eff6ff\" stroke=\"#1e293b\" stroke-width=\"2\"/>",
                x0, y0, larguraPx, alturaPx));
        desenharBisote(svg, item, x0, y0, larguraPx, alturaPx, escala);

        desenharFuracoes(svg, item.getFuracoes(), x0, y0, escala);
        for (ElementoTecnico elemento : item.getElementos()) {
            desenharElemento(svg, elemento, x0, y0, escala);
        }

        double cotaY = y0 + alturaPx + 26;
        svg.append(linhaCota(x0, cotaY, x0 + larguraPx, cotaY));
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"15\" font-weight=\"600\" fill=\"#334155\" text-anchor=\"middle\">%s mm</text>",
                x0 + larguraPx / 2, cotaY + 20, FONTE, formatarNumero(item.getLarguraFinalMm())));

        double cotaX = x0 - 24;
        svg.append(linhaCota(cotaX, y0, cotaX, y0 + alturaPx));
        svg.append(String.format(Locale.US,
                "<text x=\"0\" y=\"0\" font-family=\"%s\" font-size=\"15\" font-weight=\"600\" fill=\"#334155\" text-anchor=\"middle\" transform=\"translate(%.1f %.1f) rotate(-90)\">%s mm</text>",
                FONTE, cotaX - 16, y0 + alturaPx / 2, formatarNumero(item.getAlturaFinalMm())));

        if (item.getObservacoes() != null && !item.getObservacoes().isBlank()) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.0f\" y=\"%.0f\" font-family=\"%s\" font-size=\"12\" fill=\"#64748b\">%s</text>",
                    MARGEM_PX, svgH - 8, FONTE, escapeXml(resumo(item.getObservacoes(), 60))));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private String defsSetas() {
        return "<defs>"
                + "<marker id=\"seta-fim\" viewBox=\"0 0 10 10\" refX=\"5\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto\">"
                + "<path d=\"M0,0 L10,5 L0,10 z\" fill=\"#94a3b8\"/>"
                + "</marker>"
                + "<marker id=\"seta-inicio\" viewBox=\"0 0 10 10\" refX=\"5\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto\">"
                + "<path d=\"M10,0 L0,5 L10,10 z\" fill=\"#94a3b8\"/>"
                + "</marker>"
                + "</defs>";
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
        if (furacao.getDescricao() != null && !furacao.getDescricao().isBlank()) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" font-weight=\"600\" fill=\"#475569\" text-anchor=\"middle\">%s</text>",
                    cx, cy - raio - 5, FONTE, escapeXml(furacao.getDescricao())));
        }
        if (furacao.getDiametroMm() != null) {
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
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"11\" font-weight=\"700\" fill=\"#0f172a\" text-anchor=\"middle\">PUXADOR H</text>",
                cx, cySuperior - 13, FONTE));
    }

    private void desenharElemento(StringBuilder svg, ElementoTecnico elemento, double x0, double y0, double escala) {
        if (elemento.getPosicaoXMm() == null || elemento.getPosicaoYMm() == null || elemento.getTipo() == null) {
            return;
        }
        double cx = x0 + elemento.getPosicaoXMm().doubleValue() * escala;
        double cy = y0 + elemento.getPosicaoYMm().doubleValue() * escala;
        String cor = "#b45309";
        String rotulo = rotuloElemento(elemento);
        String medida = medidaElemento(elemento);

        switch (elemento.getTipo()) {
            case FURO -> {
                double r = elemento.getDiametroMm() != null ? Math.max(elemento.getDiametroMm().doubleValue() * escala / 2, 4) : 5;
                svg.append(String.format(Locale.US,
                        "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"#fffbeb\" stroke=\"%s\" stroke-width=\"1.6\"/>", cx, cy, r, cor));
            }
            case RASGO -> {
                double larguraPx = Math.max((elemento.getComprimentoMm() != null ? elemento.getComprimentoMm().doubleValue() : 20) * escala, 14);
                double alturaPx = Math.max((elemento.getLarguraMm() != null ? elemento.getLarguraMm().doubleValue() : 6) * escala, 6);
                svg.append(String.format(Locale.US,
                        "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" rx=\"%.1f\" fill=\"#fffbeb\" stroke=\"%s\" stroke-width=\"1.6\"/>",
                        cx - larguraPx / 2, cy - alturaPx / 2, larguraPx, alturaPx, alturaPx / 2, cor));
            }
            case RECORTE -> {
                double larguraPx = Math.max((elemento.getLarguraMm() != null ? elemento.getLarguraMm().doubleValue() : 20) * escala, 10);
                double alturaPx = Math.max((elemento.getAlturaMm() != null ? elemento.getAlturaMm().doubleValue() : 20) * escala, 10);
                svg.append(String.format(Locale.US,
                        "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"#fffbeb\" stroke=\"%s\" stroke-width=\"1.6\"/>",
                        cx - larguraPx / 2, cy - alturaPx / 2, larguraPx, alturaPx, cor));
            }
            case CHANFRO -> {
                double comp = Math.max((elemento.getComprimentoMm() != null ? elemento.getComprimentoMm().doubleValue() : 15) * escala, 8);
                svg.append(String.format(Locale.US,
                        "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"%s\" stroke-width=\"2.4\"/>",
                        cx - comp / 2, cy + comp / 2, cx + comp / 2, cy - comp / 2, cor));
            }
            case BOLEADO -> {
                double raio = Math.max((elemento.getRaioMm() != null ? elemento.getRaioMm().doubleValue() : 10) * escala, 7);
                svg.append(String.format(Locale.US,
                        "<path d=\"M %.1f %.1f A %.1f %.1f 0 0 1 %.1f %.1f\" fill=\"none\" stroke=\"%s\" stroke-width=\"2.4\"/>",
                        cx - raio, cy, raio, raio, cx, cy - raio, cor));
            }
        }

        String linha1 = medida != null ? (rotulo != null ? rotulo + " " + medida : medida) : rotulo;
        String linha2 = "X:" + formatarNumero(elemento.getPosicaoXMm()) + "mm Y:" + formatarNumero(elemento.getPosicaoYMm()) + "mm";
        if (linha1 != null) {
            svg.append(String.format(Locale.US,
                    "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"10\" font-weight=\"600\" fill=\"#92400e\" text-anchor=\"middle\">%s</text>",
                    cx, cy - 22, FONTE, escapeXml(linha1)));
        }
        svg.append(String.format(Locale.US,
                "<text x=\"%.1f\" y=\"%.1f\" font-family=\"%s\" font-size=\"9\" fill=\"#92400e\" text-anchor=\"middle\">%s</text>",
                cx, cy - 12, FONTE, escapeXml(linha2)));
    }

    private String medidaElemento(ElementoTecnico elemento) {
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