package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.CutPlanItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


@Service
public class CuttingOptimizationService {

    public static final BigDecimal DEFAULT_SAW_GAP_MM = new BigDecimal("5");

    public static class Piece {
        public String itemId;
        public String label;
        public BigDecimal width;
        public BigDecimal height;

        public Piece(String itemId, String label, BigDecimal width, BigDecimal height) {
            this.itemId = itemId; this.label = label; this.width = width; this.height = height;
        }
    }

    public static class PlacedPiece {
        public String itemId;
        public String label;
        public BigDecimal x, y, width, height;
    }

    public static class Sheet {
        public int index;
        public String groupKey;
        public BigDecimal sheetWidth;
        public BigDecimal sheetHeight;
        public List<PlacedPiece> pieces = new ArrayList<>();
        public BigDecimal usedArea = BigDecimal.ZERO;
        public BigDecimal sheetArea = BigDecimal.ZERO;
        public BigDecimal wasteArea = BigDecimal.ZERO;
        public BigDecimal utilizationPercent = BigDecimal.ZERO;
        public String svg;
    }

    public static class NestingResult {
        public List<Sheet> sheets = new ArrayList<>();
        public int totalSheets;
        public BigDecimal totalArea = BigDecimal.ZERO;
        public BigDecimal totalUsedArea = BigDecimal.ZERO;
        public BigDecimal totalWasteArea = BigDecimal.ZERO;
        public BigDecimal utilizationPercent = BigDecimal.ZERO;
    }

    /**
     * Executa a otimização para todas as peças de um Plano de Corte.
     * @param items peças do plano (cada uma expandida pela sua quantidade)
     * @param sheetWidthMm largura padrão da chapa (mm) — configurável pelo usuário/fornecedor
     * @param sheetHeightMm altura padrão da chapa (mm) — configurável pelo usuário/fornecedor
     */
    public NestingResult optimize(List<CutPlanItem> items, BigDecimal sheetWidthMm, BigDecimal sheetHeightMm) {
        BigDecimal sheetW = sheetWidthMm != null ? sheetWidthMm : new BigDecimal("2140");
        BigDecimal sheetH = sheetHeightMm != null ? sheetHeightMm : new BigDecimal("3210");

        // Agrupa por especificação de vidro (só corta junto quem é do mesmo material)
        Map<String, List<Piece>> groups = new LinkedHashMap<>();
        for (CutPlanItem item : items) {
            if (item.getFinalWidth() == null || item.getFinalHeight() == null) continue;
            String key = String.valueOf(item.getGlassType()) + "|" + item.getThickness() + "|" + item.getColor() + "|" + item.getFinish();
            int qty = item.getQuantity().setScale(0, RoundingMode.HALF_UP).intValue();
            groups.computeIfAbsent(key, k -> new ArrayList<>());
            for (int i = 0; i < Math.max(qty, 1); i++) {
                groups.get(key).add(new Piece(item.getId() != null ? item.getId().toString() : UUID.randomUUID().toString(),
                        item.getDescription(), item.getFinalWidth(), item.getFinalHeight()));
            }
        }

        NestingResult result = new NestingResult();
        int sheetCounter = 0;
        for (Map.Entry<String, List<Piece>> entry : groups.entrySet()) {
            List<Sheet> groupSheets = packGroup(entry.getKey(), entry.getValue(), sheetW, sheetH, sheetCounter);
            sheetCounter += groupSheets.size();
            result.sheets.addAll(groupSheets);
        }

        result.totalSheets = result.sheets.size();
        for (Sheet s : result.sheets) {
            result.totalArea = result.totalArea.add(s.sheetArea);
            result.totalUsedArea = result.totalUsedArea.add(s.usedArea);
            result.totalWasteArea = result.totalWasteArea.add(s.wasteArea);
        }
        if (result.totalArea.compareTo(BigDecimal.ZERO) > 0) {
            result.utilizationPercent = result.totalUsedArea.multiply(new BigDecimal("100"))
                    .divide(result.totalArea, 2, RoundingMode.HALF_UP);
        }
        return result;
    }

    private List<Sheet> packGroup(String groupKey, List<Piece> pieces, BigDecimal sheetW, BigDecimal sheetH, int startIndex) {
        // First-Fit Decreasing Height: ordena por altura decrescente
        pieces.sort((a, b) -> b.height.compareTo(a.height));

        List<Sheet> sheets = new ArrayList<>();
        List<Shelf> currentShelves = new ArrayList<>();
        Sheet currentSheet = newSheet(groupKey, sheetW, sheetH, startIndex + sheets.size() + 1);
        sheets.add(currentSheet);
        BigDecimal usedHeight = BigDecimal.ZERO;

        for (Piece p : pieces) {
            boolean placed = false;

            // tenta encaixar na prateleira existente com melhor aproveitamento (best-fit)
            Shelf bestShelf = null;
            for (Shelf shelf : currentShelves) {
                if (shelf.remainingWidth.compareTo(p.width.add(DEFAULT_SAW_GAP_MM)) >= 0
                        && p.height.compareTo(shelf.height) <= 0) {
                    if (bestShelf == null || shelf.remainingWidth.compareTo(bestShelf.remainingWidth) < 0) {
                        bestShelf = shelf;
                    }
                }
            }
            if (bestShelf != null) {
                PlacedPiece pp = new PlacedPiece();
                pp.itemId = p.itemId; pp.label = p.label; pp.width = p.width; pp.height = p.height;
                pp.x = bestShelf.usedWidth; pp.y = bestShelf.y;
                currentSheet.pieces.add(pp);
                bestShelf.usedWidth = bestShelf.usedWidth.add(p.width).add(DEFAULT_SAW_GAP_MM);
                bestShelf.remainingWidth = sheetW.subtract(bestShelf.usedWidth);
                currentSheet.usedArea = currentSheet.usedArea.add(area(p.width, p.height));
                placed = true;
            }

            if (!placed) {
                // precisa de nova prateleira — cabe na altura restante da chapa atual?
                BigDecimal neededHeight = usedHeight.add(p.height).add(DEFAULT_SAW_GAP_MM);
                if (neededHeight.compareTo(sheetH) > 0) {
                    // fecha chapa atual e abre uma nova
                    finalizeSheet(currentSheet, sheetW, sheetH);
                    currentShelves = new ArrayList<>();
                    usedHeight = BigDecimal.ZERO;
                    currentSheet = newSheet(groupKey, sheetW, sheetH, startIndex + sheets.size() + 1);
                    sheets.add(currentSheet);
                }
                Shelf shelf = new Shelf();
                shelf.y = usedHeight;
                shelf.height = p.height;
                shelf.usedWidth = p.width.add(DEFAULT_SAW_GAP_MM);
                shelf.remainingWidth = sheetW.subtract(shelf.usedWidth);
                currentShelves.add(shelf);
                usedHeight = usedHeight.add(p.height).add(DEFAULT_SAW_GAP_MM);

                PlacedPiece pp = new PlacedPiece();
                pp.itemId = p.itemId; pp.label = p.label; pp.width = p.width; pp.height = p.height;
                pp.x = BigDecimal.ZERO; pp.y = shelf.y;
                currentSheet.pieces.add(pp);
                currentSheet.usedArea = currentSheet.usedArea.add(area(p.width, p.height));
            }
        }
        finalizeSheet(currentSheet, sheetW, sheetH);
        return sheets;
    }

    private void finalizeSheet(Sheet sheet, BigDecimal sheetW, BigDecimal sheetH) {
        sheet.sheetArea = area(sheetW, sheetH);
        sheet.wasteArea = sheet.sheetArea.subtract(sheet.usedArea);
        if (sheet.sheetArea.compareTo(BigDecimal.ZERO) > 0) {
            sheet.utilizationPercent = sheet.usedArea.multiply(new BigDecimal("100"))
                    .divide(sheet.sheetArea, 2, RoundingMode.HALF_UP);
        }
        sheet.svg = renderSvg(sheet);
    }

    private Sheet newSheet(String groupKey, BigDecimal w, BigDecimal h, int index) {
        Sheet s = new Sheet();
        s.groupKey = groupKey;
        s.sheetWidth = w;
        s.sheetHeight = h;
        s.index = index;
        return s;
    }

    private BigDecimal area(BigDecimal wMm, BigDecimal hMm) {
        // mm² -> m²
        return wMm.multiply(hMm).divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);
    }

    /** Gera um SVG simples do layout da chapa (croqui de otimização), em escala. */
    private String renderSvg(Sheet sheet) {
        double scale = 300.0 / sheet.sheetWidth.doubleValue();
        double vbW = sheet.sheetWidth.doubleValue() * scale;
        double vbH = sheet.sheetHeight.doubleValue() * scale;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US,
                "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %.1f %.1f' width='100%%' height='auto'>", vbW, vbH));
        sb.append(String.format(Locale.US,
                "<rect x='0' y='0' width='%.1f' height='%.1f' fill='#f8fafc' stroke='#334155' stroke-width='2'/>", vbW, vbH));
        int colorIdx = 0;
        String[] palette = {"#bae6fd", "#bbf7d0", "#fde68a", "#fecaca", "#ddd6fe", "#fbcfe8"};
        for (PlacedPiece p : sheet.pieces) {
            double x = p.x.doubleValue() * scale;
            double y = p.y.doubleValue() * scale;
            double w = p.width.doubleValue() * scale;
            double h = p.height.doubleValue() * scale;
            String fill = palette[colorIdx++ % palette.length];
            sb.append(String.format(Locale.US,
                    "<rect x='%.1f' y='%.1f' width='%.1f' height='%.1f' fill='%s' stroke='#0f172a' stroke-width='1'/>",
                    x, y, w, h, fill));
            if (w > 30 && h > 14) {
                sb.append(String.format(Locale.US,
                        "<text x='%.1f' y='%.1f' font-size='9' fill='#0f172a'>%.0fx%.0f</text>",
                        x + 4, y + 14, p.width.doubleValue(), p.height.doubleValue()));
            }
        }
        sb.append("</svg>");
        return sb.toString();
    }

    private static class Shelf {
        BigDecimal y;
        BigDecimal height;
        BigDecimal usedWidth;
        BigDecimal remainingWidth;
    }
}
