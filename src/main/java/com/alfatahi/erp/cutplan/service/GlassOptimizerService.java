package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.entity.CutPlanItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GlassOptimizerService {

    // Dimensões padrão de chapa (em mm)
    private static final BigDecimal STANDARD_SHEET_WIDTH = new BigDecimal("3000");
    private static final BigDecimal STANDARD_SHEET_HEIGHT = new BigDecimal("2250");
    private static final BigDecimal STANDARD_SHEET_AREA = STANDARD_SHEET_WIDTH.multiply(STANDARD_SHEET_HEIGHT);


    public CutPlanOptimizationResult optimize(List<CutPlanItem> items) {
        log.info("Iniciando otimização de layout para {} itens", items.size());

        if (items.isEmpty()) {
            log.warn("Lista de itens vazia");
            return CutPlanOptimizationResult.builder()
                    .totalSheets(0)
                    .totalAreaUsed(BigDecimal.ZERO)
                    .totalAreaWasted(BigDecimal.ZERO)
                    .utilizationPercent(BigDecimal.ZERO)
                    .sheets(new ArrayList<>())
                    .build();
        }

        // 1. Criar lista de peças com suas áreas
        List<OptimizationPiece> pieces = items.stream()
                .flatMap(item -> {
                    // Criar uma entrada por unidade
                    List<OptimizationPiece> itemPieces = new ArrayList<>();
                    for (int i = 0; i < item.getQuantity(); i++) {
                        itemPieces.add(OptimizationPiece.builder()
                                .itemId(item.getId())
                                .description(item.getShortDescription())
                                .width(item.getFinalWidth())
                                .height(item.getFinalHeight())
                                .area(item.getFinalWidth().multiply(item.getFinalHeight()))
                                .build());
                    }
                    return itemPieces.stream();
                })
                .sorted(Comparator.comparing(OptimizationPiece::getArea).reversed())
                .collect(Collectors.toList());

        log.debug("Total de peças: {}", pieces.size());

        // 2. Executar algoritmo FFD
        List<OptimizationSheet> sheets = new ArrayList<>();

        for (OptimizationPiece piece : pieces) {
            boolean placed = false;

            // Tentar encaixar em chapa existente
            for (OptimizationSheet sheet : sheets) {
                if (sheet.canFit(piece)) {
                    sheet.addPiece(piece);
                    placed = true;
                    log.debug("Peça encaixada em chapa existente: {}", sheet.getId());
                    break;
                }
            }

            // Se não coube em nenhuma, criar nova chapa
            if (!placed) {
                OptimizationSheet newSheet = new OptimizationSheet();
                newSheet.addPiece(piece);
                sheets.add(newSheet);
                log.debug("Nova chapa criada. Total agora: {}", sheets.size());
            }
        }

        log.info("Otimização concluída: {} chapas utilizadas", sheets.size());

        // 3. Calcular estatísticas
        return buildOptimizationResult(sheets, pieces);
    }

    /**
     * Otimizar com limite de chapas
     * <p>
     * Se o resultado exceder o limite, retorna erro
     */
    public CutPlanOptimizationResult optimizeWithLimit(List<CutPlanItem> items, int maxSheets) {
        log.info("Otimizando com limite de {} chapas", maxSheets);

        CutPlanOptimizationResult result = optimize(items);

        if (result.getTotalSheets() > maxSheets) {
            log.warn("Limite de chapas excedido: {} > {}", result.getTotalSheets(), maxSheets);
            throw new IllegalStateException(
                    String.format(
                            "Impossível otimizar dentro do limite de %d chapas. Necessário: %d",
                            maxSheets, result.getTotalSheets()
                    )
            );
        }

        return result;
    }

    /**
     * Otimizar agrupando por tipo de vidro
     * <p>
     * Agrupa peças do mesmo tipo/espessura/cor antes de otimizar
     */
    public CutPlanOptimizationResultByGlassType optimizeByGlassType(List<CutPlanItem> items) {
        log.info("Otimizando por tipo de vidro");

        // Agrupar por tipo de vidro
        Map<String, List<CutPlanItem>> groupedByType = items.stream()
                .collect(Collectors.groupingBy(item ->
                        String.format("%s_%s_%s",
                                item.getGlassType(),
                                item.getThickness(),
                                item.getColor()
                        )
                ));

        log.debug("Encontrados {} tipos diferentes de vidro", groupedByType.size());

        Map<String, CutPlanOptimizationResult> resultsByType = new HashMap<>();
        int totalSheets = 0;
        BigDecimal totalArea = BigDecimal.ZERO;
        BigDecimal totalWaste = BigDecimal.ZERO;

        for (Map.Entry<String, List<CutPlanItem>> entry : groupedByType.entrySet()) {
            String glassType = entry.getKey();
            List<CutPlanItem> groupItems = entry.getValue();

            log.debug("Otimizando {} itens do tipo: {}", groupItems.size(), glassType);

            CutPlanOptimizationResult result = optimize(groupItems);
            resultsByType.put(glassType, result);

            totalSheets += result.getTotalSheets();
            totalArea = totalArea.add(result.getTotalAreaUsed());
            totalWaste = totalWaste.add(result.getTotalAreaWasted());
        }

        BigDecimal overallUtilization = totalArea
                .divide(STANDARD_SHEET_AREA.multiply(new BigDecimal(totalSheets)), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return CutPlanOptimizationResultByGlassType.builder()
                .resultsByType(resultsByType)
                .totalSheets(totalSheets)
                .totalAreaUsed(totalArea)
                .totalAreaWasted(totalWaste)
                .overallUtilizationPercent(overallUtilization)
                .build();
    }

    /**
     * Construir resultado da otimização
     */
    private CutPlanOptimizationResult buildOptimizationResult(
            List<OptimizationSheet> sheets, List<OptimizationPiece> pieces) {

        BigDecimal totalAreaUsed = BigDecimal.ZERO;

        for (OptimizationSheet sheet : sheets) {
            totalAreaUsed = totalAreaUsed.add(sheet.getAreaUsed());
        }

        BigDecimal totalSheetArea = STANDARD_SHEET_AREA.multiply(new BigDecimal(sheets.size()));
        BigDecimal totalAreaWasted = totalSheetArea.subtract(totalAreaUsed);

        BigDecimal utilization = totalAreaUsed
                .divide(totalSheetArea, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        log.info("Resultado: {} chapas, {:.2f}% utilização",
                sheets.size(), utilization);

        return CutPlanOptimizationResult.builder()
                .totalSheets(sheets.size())
                .totalAreaUsed(totalAreaUsed.setScale(2, RoundingMode.HALF_UP))
                .totalAreaWasted(totalAreaWasted.setScale(2, RoundingMode.HALF_UP))
                .utilizationPercent(utilization.setScale(2, RoundingMode.HALF_UP))
                .sheets(sheets)
                .build();
    }

    /**
     * Calcular número mínimo de chapas necessárias (teórico)
     * <p>
     * Útil para validação: resultado real >= mínimo teórico
     */
    public int calculateMinimumSheetsTheoretical(List<CutPlanItem> items) {
        BigDecimal totalArea = items.stream()
                .map(item -> item.getFinalWidth()
                        .multiply(item.getFinalHeight())
                        .multiply(new BigDecimal(item.getQuantity()))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Math.ceil(totalArea.divide(STANDARD_SHEET_AREA, 4, RoundingMode.HALF_UP).doubleValue())
                .intValue();
    }

    /**
     * Obter eficiência de empacotamento
     * <p>
     * eficiência = folhas teóricas / folhas reais × 100%
     */
    public BigDecimal getPackingEfficiency(List<CutPlanItem> items, int actualSheets) {
        int theoreticalSheets = calculateMinimumSheetsTheoretical(items);

        return new BigDecimal(theoreticalSheets)
                .divide(new BigDecimal(actualSheets), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Classe interna: Peça de vidro para otimização
     */
    @Data
    @Builder
    public static class OptimizationPiece {
        private UUID itemId;
        private String description;
        private BigDecimal width;
        private BigDecimal height;
        private BigDecimal area;
    }

    /**
     * Classe interna: Chapa de vidro
     */
    public static class OptimizationSheet {
        private static int counter = 1;

        private String id;
        private BigDecimal availableWidth = STANDARD_SHEET_WIDTH;
        private BigDecimal availableHeight = STANDARD_SHEET_HEIGHT;
        private List<OptimizationPiece> pieces = new ArrayList<>();
        private BigDecimal areaUsed = BigDecimal.ZERO;

        public OptimizationSheet() {
            this.id = "Sheet_" + (counter++);
        }

        /**
         * Verificar se peça cabe na chapa
         * <p>
         * Implementação simplificada: apenas verifica área
         * Em produção, seria necessário verificar também disposição 2D
         */
        public boolean canFit(OptimizationPiece piece) {
            BigDecimal remainingArea = STANDARD_SHEET_AREA.subtract(areaUsed);
            return piece.getArea().compareTo(remainingArea) <= 0;
        }

        /**
         * Adicionar peça à chapa
         */
        public void addPiece(OptimizationPiece piece) {
            pieces.add(piece);
            areaUsed = areaUsed.add(piece.getArea());
        }

        public String getId() {
            return id;
        }

        public List<OptimizationPiece> getPieces() {
            return pieces;
        }

        public BigDecimal getAreaUsed() {
            return areaUsed;
        }

        public BigDecimal getAreaWasted() {
            return STANDARD_SHEET_AREA.subtract(areaUsed);
        }

        public BigDecimal getUtilizationPercent() {
            return areaUsed.divide(STANDARD_SHEET_AREA, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }
}
