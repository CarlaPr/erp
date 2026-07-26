package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.dto.CutPlanItemSimulationResult;
import com.alfatahi.erp.cutplan.entity.CutPlanItem;
import com.alfatahi.erp.cutplan.entity.GlassCutRule;
import com.alfatahi.erp.cutplan.repository.GlassCutRuleRepository;
import com.alfatahi.erp.entity.ServiceCategory;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * TechnicalRulesService - Aplicação de regras técnicas de corte
 *
 * Responsabilidade:
 * - Aplicar descontos laterais/verticais
 * - Aplicar folgas de ferragem, silicone, instalação
 * - Recalcular dimensões finais baseado em regras
 * - Validar aplicabilidade de regras
 *
 * Fluxo:
 * 1. Buscar categoria da OS
 * 2. Buscar todas as regras da categoria
 * 3. Ordenar regras por applicationOrder
 * 4. Para cada regra:
 *    - Aplicar desconto/folga na dimensão apropriada
 *    - Recalcular área
 * 5. Registrar mudanças
 *
 * Exemplo - BOX:
 * - LATERAL_DISCOUNT: 30mm (ambos os lados = 60mm total)
 * - SUPERIOR_DISCOUNT: 20mm (ambos os lados = 40mm total)
 * - HARDWARE_GAP: 5mm (informativo)
 * - SILICONE_GAP: 3mm (informativo)
 * - INSTALL_GAP: 2mm (informativo)
 *
 * Cálculo:
 * grossWidth = 1000mm
 * - LATERAL_DISCOUNT (30mm × 2) = 1000 - 60 = 940mm ✓ finalWidth
 *
 * grossHeight = 800mm
 * - SUPERIOR_DISCOUNT (20mm × 2) = 800 - 40 = 760mm ✓ finalHeight
 */
@Slf4j
@Service
@Transactional
public class TechnicalRulesService {

    @Autowired
    private GlassCutRuleRepository glassRuleRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    /**
     * Aplicar todas as regras técnicas a um item
     *
     * @param item CutPlanItem a processar
     * @param category ServiceCategory para buscar regras
     */
    public void applyRules(CutPlanItem item, ServiceCategory category) {
        if (category == null) {
            log.warn("Categoria de serviço não fornecida. Pulando aplicação de regras.");
            return;
        }

        log.info("Aplicando regras técnicas para item: {} (Categoria: {})",
                item.getDescription(), category.getName());

        // Buscar todas as regras da categoria
        List<GlassCutRule> rules = glassRuleRepository
                .findByServiceCategoryId(category.getId());

        if (rules.isEmpty()) {
            log.warn("Nenhuma regra encontrada para categoria: {}", category.getName());
            // Neste caso, finalWidth = grossWidth e finalHeight = grossHeight
            item.setFinalWidth(item.getGrossWidth());
            item.setFinalHeight(item.getGrossHeight());
            recalculateArea(item);
            return;
        }

        log.debug("Encontradas {} regras para categoria: {}", rules.size(), category.getName());

        // Inicializar dimensões finais com dimensões brutas
        BigDecimal finalWidth = item.getGrossWidth();
        BigDecimal finalHeight = item.getGrossHeight();

        // Aplicar cada regra
        for (GlassCutRule rule : rules) {
            log.debug("Aplicando regra: {} = {} {}",
                    rule.getParameterNameLabel(), rule.getValue(), rule.getUnitLabel());

            switch (rule.getParameterName()) {
                // Descontos de largura (horizontal)
                case "LATERAL_DISCOUNT":
                    // Desconta dos dois lados (multiplicar por 2)
                    finalWidth = applyDiscountBothSides(finalWidth, rule);
                    break;

                case "PROFILE_DISCOUNT":
                    // Desconto para perfis (janelas)
                    finalWidth = applyDiscount(finalWidth, rule);
                    break;

                // Descontos de altura (vertical)
                case "SUPERIOR_DISCOUNT":
                    // Desconto na parte superior (multiplicar por 2 ou 1, conforme tipo)
                    finalHeight = applyDiscountBothSides(finalHeight, rule);
                    break;

                case "INFERIOR_DISCOUNT":
                    // Desconto na parte inferior
                    finalHeight = applyDiscountBothSides(finalHeight, rule);
                    break;

                // Folgas (não descontam da dimensão, apenas informativas)
                case "HARDWARE_GAP":
                    // Registra folga de ferragem
                    log.debug("Folga de ferragem: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "SILICONE_GAP":
                    // Registra folga de silicone
                    log.debug("Folga de silicone: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "INSTALL_GAP":
                    // Registra folga de instalação
                    log.debug("Folga de instalação: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                // Regras específicas de janelas
                case "TRACK_GAP":
                    log.debug("Folga de trilho: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "ROLLER_GAP":
                    log.debug("Folga de roldana: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "SEAL_GAP":
                    log.debug("Folga de vedação: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                // Regras específicas de espelhos
                case "POLISH_GAP":
                    log.debug("Folga de polimento: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "BEVEL_GAP":
                    log.debug("Folga de biselado: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "LED_SPACE":
                    log.debug("Espaço para LED: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                // Regras específicas de guarda-corpo
                case "SUPPORT_GAP":
                    log.debug("Folga de suporte: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                case "BUTTON_GAP":
                    log.debug("Folga de botão: {}{}", rule.getValue(), rule.getUnitLabel());
                    break;

                default:
                    log.warn("Parâmetro de regra não reconhecido: {}", rule.getParameterName());
            }
        }

        // Validar dimensões mínimas
        if (finalWidth.compareTo(BigDecimal.ZERO) <= 0 ||
                finalHeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Dimensões inválidas após aplicar regras. " +
                                    "Largura: %.2fmm, Altura: %.2fmm",
                            finalWidth, finalHeight
                    )
            );
        }

        // Atualizar item
        item.setFinalWidth(finalWidth);
        item.setFinalHeight(finalHeight);

        // Recalcular área
        recalculateArea(item);

        log.info("Regras aplicadas com sucesso. Dimensões: {}x{}mm → {}x{}mm",
                item.getGrossWidth(), item.getGrossHeight(),
                item.getFinalWidth(), item.getFinalHeight());
    }

    /**
     * Aplicar desconto em ambos os lados (ex: 30mm em cada lado = 60mm total)
     */
    private BigDecimal applyDiscountBothSides(BigDecimal value, GlassCutRule rule) {
        if ("MM".equals(rule.getUnit())) {
            // Em milímetros: desconta 2 × value
            BigDecimal totalDiscount = rule.getValue().multiply(new BigDecimal("2"));
            return value.subtract(totalDiscount);
        } else if ("PERCENT".equals(rule.getUnit()) || "PCT".equals(rule.getUnit())) {
            // Em percentual
            BigDecimal percentage = rule.getValue().divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP);
            return value.multiply(percentage);
        }
        return value;
    }

    /**
     * Aplicar desconto simples
     */
    private BigDecimal applyDiscount(BigDecimal value, GlassCutRule rule) {
        if ("MM".equals(rule.getUnit())) {
            return value.subtract(rule.getValue());
        } else if ("PERCENT".equals(rule.getUnit()) || "PCT".equals(rule.getUnit())) {
            BigDecimal percentage = rule.getValue().divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP);
            return value.multiply(percentage);
        }
        return value;
    }

    /**
     * Recalcular área total do item (finalWidth × finalHeight × quantity)
     */
    private void recalculateArea(CutPlanItem item) {
        BigDecimal area = item.getFinalWidth()
                .multiply(item.getFinalHeight())
                .multiply(new BigDecimal(item.getQuantity()));

        item.setCalculatedArea(area);

        log.debug("Área recalculada: {}mm² ({} × {} × {})",
                area, item.getFinalWidth(), item.getFinalHeight(), item.getQuantity());
    }

    /**
     * Obter regra específica
     */
    public GlassCutRule getRule(UUID categoryId, String parameterName) {
        return glassRuleRepository.findByServiceCategoryIdAndParameterName(categoryId, parameterName)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Regra não encontrada: %s para categoria: %s",
                                parameterName, categoryId)
                ));
    }

    /**
     * Validar se regra existe para categoria
     */
    public boolean ruleExists(UUID categoryId, String parameterName) {
        return glassRuleRepository.existsByServiceCategoryIdAndParameterNameAndActiveTrue(
                categoryId, parameterName);
    }

    /**
     * Obter todas as regras de uma categoria
     */
    public List<GlassCutRule> getRulesByCategory(UUID categoryId) {
        return glassRuleRepository.findByServiceCategoryId(categoryId);
    }

    /**
     * Obter apenas regras de desconto
     */
    public List<GlassCutRule> getDiscountRules(UUID categoryId) {
        return glassRuleRepository.findDiscountRules(categoryId);
    }

    /**
     * Obter apenas regras de folga
     */
    public List<GlassCutRule> getGapRules(UUID categoryId) {
        return glassRuleRepository.findGapRules(categoryId);
    }

    /**
     * Simular aplicação de regras sem salvar
     * Útil para preview antes de confirmar
     */
    public CutPlanItemSimulationResult simulateRulesApplication(
            CutPlanItem item, UUID categoryId) {

        log.debug("Simulando aplicação de regras para item: {}", item.getDescription());

        ServiceCategory category = serviceCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Categoria não encontrada: " + categoryId));

        // Criar cópia do item para simulação
        CutPlanItem simulated = new CutPlanItem();
        simulated.setGrossWidth(item.getGrossWidth());
        simulated.setGrossHeight(item.getGrossHeight());
        simulated.setQuantity(item.getQuantity());

        // Aplicar regras
        applyRules(simulated, category);

        return CutPlanItemSimulationResult.builder()
                .originalWidth(item.getGrossWidth())
                .originalHeight(item.getGrossHeight())
                .finalWidth(simulated.getFinalWidth())
                .finalHeight(simulated.getFinalHeight())
                .widthReduction(item.getGrossWidth().subtract(simulated.getFinalWidth()))
                .heightReduction(item.getGrossHeight().subtract(simulated.getFinalHeight()))
                .areaReduction(item.getGrossWidth()
                        .multiply(item.getGrossHeight())
                        .multiply(new BigDecimal(item.getQuantity()))
                        .subtract(simulated.getCalculatedArea()))
                .build();
    }

    /**
     * Aplicar regras para múltiplos items em lote
     */
    public void applyRulesInBatch(List<CutPlanItem> items, ServiceCategory category) {
        log.info("Aplicando regras em lote para {} itens", items.size());

        for (CutPlanItem item : items) {
            try {
                applyRules(item, category);
            } catch (Exception e) {
                log.error("Erro ao aplicar regras para item: {}", item.getDescription(), e);
                throw new RuntimeException(
                        String.format("Erro ao aplicar regras para item: %s", item.getDescription()), e
                );
            }
        }

        log.info("Regras aplicadas com sucesso para {} itens", items.size());
    }
}