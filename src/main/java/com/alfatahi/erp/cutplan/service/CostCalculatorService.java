package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.dto.CutPlanCostBreakdown;
import com.alfatahi.erp.cutplan.dto.CutPlanItemCostSimulation;
import com.alfatahi.erp.cutplan.entity.*;
import com.alfatahi.erp.cutplan.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * CostCalculatorService - Cálculo de custos de itens de vidro
 *
 * Responsabilidade:
 * - Calcular custo de vidro (base + ajustes)
 * - Calcular custo de ferragens (dobradiças, puxadores, etc)
 * - Calcular custo de alumínio/perfis
 * - Calcular custo de silicone
 * - Calcular custo de acabamentos e serviços
 * - Obter preços vigentes da tabela de custos
 *
 * Fórmulas de Cálculo:
 *
 * 1. CUSTO DE VIDRO:
 *    base = (área em m²) × (preço por m²) × quantidade
 *    ajuste_tipo = ajuste_por_tipo_vidro
 *    ajuste_acabamento = ajuste_por_acabamento
 *    total = base + ajuste_tipo + ajuste_acabamento
 *
 * 2. CUSTO DE FERRAGEM:
 *    total = SUM(quantidade × preço_unitário) para cada ferragem
 *
 * 3. CUSTO DE ALUMÍNIO:
 *    total = metros_lineares × preço_por_metro
 *
 * 4. CUSTO DE SILICONE:
 *    total = (perímetro / 600) × quantidade_tubos × preço_tubo
 *
 * 5. CUSTO FINAL:
 *    total = custo_vidro + custo_ferragem + custo_alumínio + custo_silicone
 */
@Slf4j
@Service
@Transactional
public class CostCalculatorService {

    @Autowired
    private CostTableRepository costTableRepository;

    @Autowired
    private GlassFinishingRepository glassFinishingRepository;

    @Autowired
    private GlassDrillingRepository glassDrillingRepository;

    @Autowired
    private GlassNotchRepository glassNotchRepository;

    // Constantes de cálculo
    private static final BigDecimal SILICONE_TUBE_METERS = new BigDecimal("600");
    private static final BigDecimal GLASS_DENSITY_KG_M3 = new BigDecimal("2500");

    /**
     * Calcular custo total de um item
     *
     * Fluxo:
     * 1. Calcular custo de vidro
     * 2. Calcular custo de ferragens
     * 3. Calcular custo de alumínio
     * 4. Calcular custo de silicone
     * 5. Somar todos os custos
     * 6. Registrar no item
     */
    public void calculateItemCost(CutPlanItem item) {
        log.info("Calculando custo para item: {}", item.getDescription());

        try {
            // Calcular custos parciais
            BigDecimal glassCost = calculateGlassCost(item);
            BigDecimal hardwareCost = calculateHardwareCost(item);
            BigDecimal aluminumCost = calculateAluminumCost(item);
            BigDecimal siliconeCost = calculateSiliconeCost(item);
            BigDecimal drillingCost = calculateDrillingCost(item);
            BigDecimal notchCost = calculateNotchCost(item);

            // Calcular peso estimado
            calculateEstimatedWeight(item);

            // Atualizar item
            item.setGlassCost(glassCost);
            item.setHardwaresTotalCost(hardwareCost);
            item.setAluminumTotalCost(aluminumCost);
            item.setSiliconeTotalCost(siliconeCost);

            // Somar todos os custos
            BigDecimal totalCost = glassCost
                    .add(hardwareCost)
                    .add(aluminumCost)
                    .add(siliconeCost)
                    .add(drillingCost)
                    .add(notchCost);

            item.setEstimatedCost(totalCost);

            log.info("Custo calculado: R$ {:.2f} (Vidro: {:.2f}, Ferragem: {:.2f}, " +
                            "Alumínio: {:.2f}, Silicone: {:.2f}, Furos: {:.2f}, Entalhes: {:.2f})",
                    totalCost, glassCost, hardwareCost, aluminumCost, siliconeCost,
                    drillingCost, notchCost);

        } catch (Exception e) {
            log.error("Erro ao calcular custo para item: {}", item.getDescription(), e);
            throw new RuntimeException(
                    String.format("Erro ao calcular custo para item: %s", item.getDescription()), e
            );
        }
    }

    /**
     * Calcular custo de vidro
     *
     * Fórmula:
     * 1. Obter preço por m² do tipo/espessura/cor
     * 2. Converter área para m²
     * 3. Calcular base = área × preço × quantidade
     * 4. Aplicar ajustes por tipo de vidro
     * 5. Aplicar ajustes por acabamento
     */
    private BigDecimal calculateGlassCost(CutPlanItem item) {
        log.debug("Calculando custo de vidro para item: {}", item.getDescription());

        // Construir chave de busca
        String itemType = String.format("%s_%s_%s",
                item.getGlassType().toUpperCase(),
                item.getThickness().toPlainString().replace(".", "_"),
                item.getColor() != null ? item.getColor().toUpperCase() : "TRANSPARENTE"
        );

        // Buscar preço vigente
        Optional<CostTable> priceTable = costTableRepository.findCurrentPrice("GLASS", itemType);

        if (priceTable.isEmpty()) {
            log.warn("Nenhum preço encontrado para vidro: {}. Usando preço zero.", itemType);
            return BigDecimal.ZERO;
        }

        CostTable price = priceTable.get();

        // Calcular área em m²
        BigDecimal areaInM2 = item.getAreaInSquareMeters();

        // Base = área × preço unitário × quantidade
        BigDecimal baseCost = areaInM2
                .multiply(price.getUnitPrice())
                .multiply(new BigDecimal(item.getQuantity()));

        log.debug("Custo base de vidro: R$ {:.2f} ({} m² × R$ {}/m² × {})",
                baseCost, areaInM2, price.getUnitPrice(), item.getQuantity());

        // Aplicar ajustes por acabamento
        BigDecimal adjustmentCost = BigDecimal.ZERO;
        if (item.getFinishing() != null && !item.getFinishing().isEmpty()) {
            Optional<GlassFinishing> finishing = glassFinishingRepository.findByName(item.getFinishing());

            if (finishing.isPresent()) {
                GlassFinishing f = finishing.get();
                if ("FIXED".equals(f.getAdjustmentType())) {
                    // Custo fixo por m²
                    adjustmentCost = f.getCostAdjustment().multiply(areaInM2);
                } else if ("PERCENTAGE".equals(f.getAdjustmentType())) {
                    // Percentual sobre custo base
                    BigDecimal percentage = f.getCostAdjustment()
                            .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                    adjustmentCost = baseCost.multiply(percentage);
                }
                log.debug("Ajuste de acabamento ({}): R$ {:.2f}",
                        item.getFinishing(), adjustmentCost);
            }
        }

        return baseCost.add(adjustmentCost).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcular custo de ferragens
     *
     * Nota: Implementação depende de como ferragens são armazenadas no item
     * Pode ser um JSON array ou relacionamento N:M
     *
     * Simplificado: Retorna zero (será implementado quando estrutura for clara)
     */
    private BigDecimal calculateHardwareCost(CutPlanItem item) {
        log.debug("Calculando custo de ferragens para item: {}", item.getDescription());

        // Placeholder: Retornar zero
        // Em produção, buscar ferragens relacionadas ao item
        // e somar seus custos

        return BigDecimal.ZERO;
    }

    /**
     * Calcular custo de alumínio/perfis
     *
     * Nota: Implementação depende de como perfis são armazenados
     *
     * Simplificado: Retorna zero
     */
    private BigDecimal calculateAluminumCost(CutPlanItem item) {
        log.debug("Calculando custo de alumínio para item: {}", item.getDescription());

        // Placeholder: Retornar zero
        // Em produção, buscar perfis relacionados ao item
        // metros_lineares × preço_por_metro

        return BigDecimal.ZERO;
    }

    /**
     * Calcular custo de silicone
     *
     * Fórmula:
     * perímetro = 2 × (largura + altura)
     * tubos_necessários = CEIL(perímetro / 600mm)
     * custo = tubos × preço_tubo
     *
     * Nota: Apenas para certos tipos de item (ex: box, janela)
     */
    private BigDecimal calculateSiliconeCost(CutPlanItem item) {
        log.debug("Calculando custo de silicone para item: {}", item.getDescription());

        // Apenas tipos específicos usam silicone
        if (item.getFinalWidth() == null || item.getFinalHeight() == null) {
            return BigDecimal.ZERO;
        }

        // Calcular perímetro em mm
        BigDecimal perimeter = item.getFinalWidth()
                .add(item.getFinalHeight())
                .multiply(new BigDecimal("2"));

        // Calcular número de tubos necessários (600mm por tubo)
        BigDecimal tubesNeeded = perimeter
                .divide(SILICONE_TUBE_METERS, 2, RoundingMode.UP);

        // Buscar preço de silicone
        Optional<CostTable> priceTable = costTableRepository.findCurrentPrice("SILICONE", "SILICONE_600ML");

        if (priceTable.isEmpty()) {
            log.warn("Preço de silicone não encontrado");
            return BigDecimal.ZERO;
        }

        BigDecimal siliconeCost = tubesNeeded.multiply(priceTable.get().getUnitPrice())
                .multiply(new BigDecimal(item.getQuantity()));

        log.debug("Custo de silicone: R$ {:.2f} ({} tubos × R$ {}/tubo)",
                siliconeCost, tubesNeeded, priceTable.get().getUnitPrice());

        return siliconeCost.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcular custo de furos (furações)
     */
    private BigDecimal calculateDrillingCost(CutPlanItem item) {
        if (item.getDrillingQuantity() == null || item.getDrillingQuantity() == 0) {
            return BigDecimal.ZERO;
        }

        log.debug("Calculando custo de furos: {} furos", item.getDrillingQuantity());

        if (item.getDrillingCostPerUnit() == null) {
            // Buscar no catálogo
            BigDecimal drillingCost = BigDecimal.ZERO;
            if (item.getDrillingDiameter() != null) {
                Optional<GlassDrilling> drilling = glassDrillingRepository
                        .findByDiameter(item.getDrillingDiameter())
                        .stream()
                        .findFirst();

                if (drilling.isPresent()) {
                    drillingCost = drilling.get().getCostPerUnit();
                }
            }

            return drillingCost
                    .multiply(new BigDecimal(item.getDrillingQuantity()))
                    .multiply(new BigDecimal(item.getQuantity()));
        }

        return item.getDrillingCostPerUnit()
                .multiply(new BigDecimal(item.getDrillingQuantity()))
                .multiply(new BigDecimal(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcular custo de entalhes (rebaixos)
     */
    private BigDecimal calculateNotchCost(CutPlanItem item) {
        if (item.getNotchCost() != null) {
            return item.getNotchCost()
                    .multiply(new BigDecimal(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Calcular peso estimado do item
     *
     * Fórmula:
     * peso = (área em m²) × (espessura em mm) × (densidade em kg/m³) × quantidade
     *
     * Densidades típicas:
     * - Vidro comum: 2.500 kg/m³
     * - Vidro temperado: 2.500 kg/m³
     */
    private void calculateEstimatedWeight(CutPlanItem item) {
        if (item.getFinalWidth() == null || item.getFinalHeight() == null ||
                item.getThickness() == null) {
            return;
        }

        // Área em m²
        BigDecimal areaInM2 = item.getAreaInSquareMeters();

        // Peso por m² = thickness(mm) × density(kg/m³) / 1000
        BigDecimal weightPerM2 = item.getThickness()
                .multiply(GLASS_DENSITY_KG_M3)
                .divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);

        // Peso total = área × peso por m² × quantidade
        BigDecimal totalWeight = areaInM2
                .multiply(weightPerM2)
                .multiply(new BigDecimal(item.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        item.setEstimatedWeight(totalWeight);

        log.debug("Peso estimado: {} kg ({} m² × {}kg/m² × {})",
                totalWeight, areaInM2, weightPerM2, item.getQuantity());
    }

    /**
     * Recalcular custos para múltiplos items
     */
    public void calculateItemsInBatch(List<CutPlanItem> items) {
        log.info("Calculando custos em lote para {} itens", items.size());

        for (CutPlanItem item : items) {
            calculateItemCost(item);
        }

        log.info("Custos calculados com sucesso para {} itens", items.size());
    }

    /**
     * Obter preço vigente de vidro
     */
    public BigDecimal getGlassPrice(String glassType, BigDecimal thickness, String color) {
        String itemType = String.format("%s_%s_%s",
                glassType.toUpperCase(),
                thickness.toPlainString().replace(".", "_"),
                color != null ? color.toUpperCase() : "TRANSPARENTE"
        );

        Optional<CostTable> price = costTableRepository.findCurrentPrice("GLASS", itemType);
        return price.isPresent() ? price.get().getUnitPrice() : BigDecimal.ZERO;
    }

    /**
     * Obter preço de silicone
     */
    public BigDecimal getSiliconePrice() {
        Optional<CostTable> price = costTableRepository.findCurrentPrice("SILICONE", "SILICONE_600ML");
        return price.isPresent() ? price.get().getUnitPrice() : BigDecimal.ZERO;
    }

    /**
     * Simular cálculo de custo sem salvar
     */
    public CutPlanItemCostSimulation simulateCostCalculation(CutPlanItem item) {
        log.debug("Simulando cálculo de custo para item: {}", item.getDescription());

        BigDecimal glassCost = calculateGlassCost(item);
        BigDecimal hardwareCost = calculateHardwareCost(item);
        BigDecimal aluminumCost = calculateAluminumCost(item);
        BigDecimal siliconeCost = calculateSiliconeCost(item);
        BigDecimal drillingCost = calculateDrillingCost(item);
        BigDecimal notchCost = calculateNotchCost(item);

        BigDecimal totalCost = glassCost
                .add(hardwareCost)
                .add(aluminumCost)
                .add(siliconeCost)
                .add(drillingCost)
                .add(notchCost);

        return CutPlanItemCostSimulation.builder()
                .itemDescription(item.getDescription())
                .glassCost(glassCost)
                .hardwareCost(hardwareCost)
                .aluminumCost(aluminumCost)
                .siliconeCost(siliconeCost)
                .drillingCost(drillingCost)
                .notchCost(notchCost)
                .totalCost(totalCost)
                .build();
    }

    /**
     * Obter breakdown de custos por categoria
     */
    public CutPlanCostBreakdown getCostBreakdown(CutPlanItem item) {
        BigDecimal glassPercentage = calculateGlassCost(item)
                .divide(item.getEstimatedCost(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        BigDecimal hardwarePercentage = calculateHardwareCost(item)
                .divide(item.getEstimatedCost(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        BigDecimal aluminumPercentage = calculateAluminumCost(item)
                .divide(item.getEstimatedCost(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        BigDecimal siliconePercentage = calculateSiliconeCost(item)
                .divide(item.getEstimatedCost(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return CutPlanCostBreakdown.builder()
                .itemDescription(item.getDescription())
                .totalCost(item.getEstimatedCost())
                .glassPercentage(glassPercentage)
                .hardwarePercentage(hardwarePercentage)
                .aluminumPercentage(aluminumPercentage)
                .siliconePercentage(siliconePercentage)
                .build();
    }
}

