package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.CutPlanItem;
import com.alfatahi.erp.entity.CutPlanItemChamfer;
import com.alfatahi.erp.entity.CutPlanItemDrilling;
import com.alfatahi.erp.entity.CutPlanItemNotch;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;


@Service
public class GlassDrawingService {

    private static final double MAX_DRAWING_PX = 320;
    private static final double MARGIN_LEFT = 60;
    private static final double MARGIN_TOP = 78;
    private static final double MARGIN_BOTTOM = 46;
    private static final double MARGIN_RIGHT = 30;

    public String render(CutPlanItem item, int pieceNumber) {
        BigDecimal wMm = item.getFinalWidth() != null ? item.getFinalWidth() : BigDecimal.ZERO;
        BigDecimal hMm = item.getFinalHeight() != null ? item.getFinalHeight() : BigDecimal.ZERO;
        double w = wMm.doubleValue();
        double h = hMm.doubleValue();
        if (w <= 0) w = 100;
        if (h <= 0) h = 100;

        double scale = MAX_DRAWING_PX / Math.max(w, h);
        double pieceWpx = w * scale;
        double pieceHpx = h * scale;
        double svgW = MARGIN_LEFT + pieceWpx + MARGIN_RIGHT;
        double svgH = MARGIN_TOP + pieceHpx + MARGIN_BOTTOM;

        Map<CutPlanItemChamfer.Corner, Double> chamferByCorner = new EnumMap<>(CutPlanItemChamfer.Corner.class);
        for (CutPlanItemChamfer c : item.getChamfers()) {
            if (c.getCorner() != null && c.getSize() != null) chamferByCorner.put(c.getCorner(), c.getSize().doubleValue());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %.1f %.1f' width='100%%' height='auto' font-family='Arial, sans-serif'>",
                svgW, svgH));

        // ── Cabeçalho de identificação ──
        String code = item.getId() != null ? item.getId().toString().substring(0, 8).toUpperCase() : "-";
        sb.append(String.format(Locale.US, "<text x='4' y='14' font-size='11' font-weight='bold' fill='#0f172a'>PEÇA %02d — %s</text>",
                pieceNumber, escape(nullToDash(item.getDescription()))));
        sb.append(String.format(Locale.US, "<text x='4' y='28' font-size='8.5' fill='#475569'>Código: %s · %s %smm %s %s</text>",
                code, escape(nullToDash(item.getGlassType())), item.getThickness() != null ? item.getThickness().toPlainString() : "-",
                escape(nullToDash(item.getColor())), escape(nullToDash(item.getFinish()))));
        sb.append(String.format(Locale.US, "<text x='4' y='40' font-size='8.5' fill='#475569'>Qtd: %s · Ângulo: %s · Borda: %s</text>",
                item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "1",
                escape(nullToDash(item.getAngleType())), escape(nullToDash(item.getEdgeWork()))));
        if (item.getObservations() != null && !item.getObservations().isBlank()) {
            sb.append(String.format(Locale.US, "<text x='4' y='52' font-size='8' fill='#64748b'>Obs: %s</text>",
                    escape(truncate(item.getObservations(), 80))));
        }

        // ── Contorno da peça (com chanfros de canto) ──
        List<double[]> poly = buildOutline(w, h, chamferByCorner);
        StringBuilder pts = new StringBuilder();
        for (double[] p : poly) {
            double[] svgP = toSvg(p[0], p[1], pieceHpx, scale);
            pts.append(svgP[0]).append(",").append(svgP[1]).append(" ");
        }
        sb.append(String.format(Locale.US,
                "<polygon points='%s' fill='#eff6ff' stroke='#1e293b' stroke-width='1.6'/>", pts.toString().trim()));

        // ── Recortes ──
        for (CutPlanItemNotch n : item.getNotches()) {
            if (n.getPosX() == null || n.getPosY() == null || n.getWidth() == null || n.getHeight() == null) continue;
            double nx = n.getPosX().doubleValue(), ny = n.getPosY().doubleValue();
            double nw = n.getWidth().doubleValue(), nh = n.getHeight().doubleValue();
            double[] topLeftMm = {nx, ny + nh};
            double[] svgP = toSvg(topLeftMm[0], topLeftMm[1], pieceHpx, scale);
            sb.append(String.format(Locale.US,
                    "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' fill='#ffffff' stroke='#dc2626' stroke-width='1' stroke-dasharray='3,2'/>",
                    svgP[0], svgP[1], nw * scale, nh * scale));
            sb.append(String.format(Locale.US,
                    "<text x='%.1f' y='%.1f' font-size='6.5' fill='#dc2626'>%.0fx%.0f</text>",
                    svgP[0] + 2, svgP[1] + (nh * scale) / 2, nw, nh));
        }

        // ── Furos ──
        for (CutPlanItemDrilling d : item.getDrillings()) {
            if (d.getPosX() == null || d.getPosY() == null || d.getDiameter() == null) continue;
            double[] center = toSvg(d.getPosX().doubleValue(), d.getPosY().doubleValue(), pieceHpx, scale);
            double r = Math.max(2, d.getDiameter().doubleValue() * scale / 2);
            sb.append(String.format(Locale.US,
                    "<circle cx='%.1f' cy='%.1f' r='%.1f' fill='#ffffff' stroke='#0f172a' stroke-width='1'/>",
                    center[0], center[1], r));
            sb.append(String.format(Locale.US,
                    "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#0f172a' stroke-width='0.6'/>",
                    center[0] - r - 2, center[1], center[0] + r + 2, center[1]));
            sb.append(String.format(Locale.US,
                    "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#0f172a' stroke-width='0.6'/>",
                    center[0], center[1] - r - 2, center[0], center[1] + r + 2));
            sb.append(String.format(Locale.US,
                    "<text x='%.1f' y='%.1f' font-size='6.5' fill='#334155'>⌀%s</text>",
                    center[0] + r + 3, center[1] - r, d.getDiameter().stripTrailingZeros().toPlainString()));
        }

        // ── Linhas de cota ──
        double baseY = MARGIN_TOP + pieceHpx + 18;
        sb.append(String.format(Locale.US, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#475569' stroke-width='0.7'/>",
                MARGIN_LEFT, baseY, MARGIN_LEFT + pieceWpx, baseY));
        sb.append(String.format(Locale.US, "<text x='%.1f' y='%.1f' font-size='8' fill='#334155' text-anchor='middle'>%s mm</text>",
                MARGIN_LEFT + pieceWpx / 2, baseY + 11, wMm.stripTrailingZeros().toPlainString()));

        double baseX = MARGIN_LEFT - 14;
        sb.append(String.format(Locale.US, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='#475569' stroke-width='0.7'/>",
                baseX, MARGIN_TOP, baseX, MARGIN_TOP + pieceHpx));
        sb.append(String.format(Locale.US,
                "<text x='%.1f' y='%.1f' font-size='8' fill='#334155' text-anchor='middle' transform='rotate(-90 %.1f %.1f)'>%s mm</text>",
                baseX - 9, MARGIN_TOP + pieceHpx / 2, baseX - 9, MARGIN_TOP + pieceHpx / 2, hMm.stripTrailingZeros().toPlainString()));

        sb.append("</svg>");
        return sb.toString();
    }

    /** Converte um ponto (x,y) em mm — origem no canto inferior esquerdo da peça — para coordenadas SVG (y para baixo). */
    private double[] toSvg(double xMm, double yMm, double pieceHpx, double scale) {
        return new double[] { MARGIN_LEFT + xMm * scale, MARGIN_TOP + pieceHpx - yMm * scale };
    }

    /** Monta o contorno (polígono) da peça em mm, aplicando os chanfros de canto configurados. */
    private List<double[]> buildOutline(double w, double h, Map<CutPlanItemChamfer.Corner, Double> chamfer) {
        double sTL = chamfer.getOrDefault(CutPlanItemChamfer.Corner.TOP_LEFT, 0.0);
        double sTR = chamfer.getOrDefault(CutPlanItemChamfer.Corner.TOP_RIGHT, 0.0);
        double sBR = chamfer.getOrDefault(CutPlanItemChamfer.Corner.BOTTOM_RIGHT, 0.0);
        double sBL = chamfer.getOrDefault(CutPlanItemChamfer.Corner.BOTTOM_LEFT, 0.0);

        List<double[]> pts = new ArrayList<>();
        pts.add(new double[]{sTL, h});             // TL - saída (aresta superior)
        pts.add(new double[]{w - sTR, h});          // TR - entrada (aresta superior)
        pts.add(new double[]{w, h - sTR});          // TR - saída (aresta direita)
        pts.add(new double[]{w, sBR});              // BR - entrada (aresta direita)
        pts.add(new double[]{w - sBR, 0});          // BR - saída (aresta inferior)
        pts.add(new double[]{sBL, 0});              // BL - entrada (aresta inferior)
        pts.add(new double[]{0, sBL});              // BL - saída (aresta esquerda)
        pts.add(new double[]{0, h - sTL});          // TL - entrada (aresta esquerda)
        return pts;
    }

    private String nullToDash(String s) { return s == null || s.isBlank() ? "-" : s; }
    private String truncate(String s, int max) { return s.length() > max ? s.substring(0, max) + "…" : s; }
    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
