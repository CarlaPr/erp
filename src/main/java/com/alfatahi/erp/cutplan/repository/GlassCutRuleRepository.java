package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.GlassCutRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GlassCutRuleRepository - Acesso a dados de Regras Técnicas de Corte
 *
 * Fornece queries especializadas para recuperar regras por categoria,
 * tipo de parâmetro e ordem de aplicação
 */
@Repository
public interface GlassCutRuleRepository extends JpaRepository<GlassCutRule, UUID> {

    /**
     * Busca todas as regras ativas para uma categoria de serviço
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND gcr.active = true " +
                    "ORDER BY gcr.applicationOrder ASC"
    )
    List<GlassCutRule> findByServiceCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Busca todas as regras (ativas e inativas) para uma categoria
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "ORDER BY gcr.applicationOrder ASC"
    )
    List<GlassCutRule> findByServiceCategoryIdIncludingInactive(@Param("categoryId") UUID categoryId);

    /**
     * Busca uma regra específica por parâmetro e categoria
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND gcr.parameterName = :parameterName " +
                    "  AND gcr.active = true"
    )
    Optional<GlassCutRule> findByServiceCategoryIdAndParameterName(
            @Param("categoryId") UUID categoryId,
            @Param("parameterName") String parameterName
    );

    /**
     * Busca regras de um tipo específico
     */
    List<GlassCutRule> findByRuleTypeAndActiveTrue(String ruleType);

    /**
     * Busca regras de desconto (DISCOUNT)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND gcr.ruleType = 'DISCOUNT' " +
                    "  AND gcr.active = true " +
                    "ORDER BY gcr.applicationOrder ASC"
    )
    List<GlassCutRule> findDiscountRules(@Param("categoryId") UUID categoryId);

    /**
     * Busca regras de folga (GAP)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND gcr.ruleType = 'GAP' " +
                    "  AND gcr.active = true " +
                    "ORDER BY gcr.applicationOrder ASC"
    )
    List<GlassCutRule> findGapRules(@Param("categoryId") UUID categoryId);

    /**
     * Busca regras de ajuste (ADJUSTMENT)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND gcr.ruleType = 'ADJUSTMENT' " +
                    "  AND gcr.active = true " +
                    "ORDER BY gcr.applicationOrder ASC"
    )
    List<GlassCutRule> findAdjustmentRules(@Param("categoryId") UUID categoryId);

    /**
     * Busca regra específica por ID com categoria
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "LEFT JOIN FETCH gcr.serviceCategory " +
                    "WHERE gcr.id = :id"
    )
    Optional<GlassCutRule> findByIdWithCategory(@Param("id") UUID id);

    /**
     * Busca todas as regras de uma categoria com paginação
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "ORDER BY gcr.applicationOrder ASC, gcr.parameterName ASC"
    )
    List<GlassCutRule> findAllByServiceCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Conta regras por categoria
     */
    long countByServiceCategoryId(UUID categoryId);

    /**
     * Conta regras ativas por categoria
     */
    @Query(
            "SELECT COUNT(gcr) FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND gcr.active = true"
    )
    long countActiveByServiceCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Busca regras por unidade de medida
     */
    List<GlassCutRule> findByUnitAndActiveTrue(String unit);

    /**
     * Busca regras em milímetros (MM)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.unit = 'MM' " +
                    "  AND gcr.active = true " +
                    "ORDER BY gcr.value DESC"
    )
    List<GlassCutRule> findMillimeterRules();

    /**
     * Busca regras em percentual (PERCENT ou PCT)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE (gcr.unit = 'PERCENT' OR gcr.unit = 'PCT') " +
                    "  AND gcr.active = true " +
                    "ORDER BY gcr.value DESC"
    )
    List<GlassCutRule> findPercentageRules();

    /**
     * Verifica se existe regra para um parâmetro específico
     */
    boolean existsByServiceCategoryIdAndParameterNameAndActiveTrue(
            UUID categoryId,
            String parameterName
    );

    /**
     * Busca regra por nome de parâmetro (amigável para logging)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.parameterName = :parameterName " +
                    "  AND gcr.active = true"
    )
    List<GlassCutRule> findByParameterName(@Param("parameterName") String parameterName);

    /**
     * Busca regras que afetam altura (DISCOUNT com SUPERIOR/INFERIOR)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND (gcr.parameterName LIKE '%SUPERIOR%' " +
                    "       OR gcr.parameterName LIKE '%INFERIOR%' " +
                    "       OR gcr.parameterName LIKE '%HEIGHT%') " +
                    "  AND gcr.active = true"
    )
    List<GlassCutRule> findHeightRules(@Param("categoryId") UUID categoryId);

    /**
     * Busca regras que afetam largura (DISCOUNT com LATERAL)
     */
    @Query(
            "SELECT gcr FROM GlassCutRule gcr " +
                    "WHERE gcr.serviceCategory.id = :categoryId " +
                    "  AND (gcr.parameterName LIKE '%LATERAL%' " +
                    "       OR gcr.parameterName LIKE '%WIDTH%') " +
                    "  AND gcr.active = true"
    )
    List<GlassCutRule> findWidthRules(@Param("categoryId") UUID categoryId);

    /**
     * Relatório: Regras por categoria
     */
    @Query(
            "SELECT gcr.serviceCategory.name, COUNT(gcr), " +
                    "       SUM(CASE WHEN gcr.active = true THEN 1 ELSE 0 END) " +
                    "FROM GlassCutRule gcr " +
                    "GROUP BY gcr.serviceCategory.id, gcr.serviceCategory.name " +
                    "ORDER BY gcr.serviceCategory.name"
    )
    List<Object[]> getStatsByCategory();

    /**
     * Relatório: Distribuição por tipo de regra
     */
    @Query(
            "SELECT gcr.ruleType, COUNT(gcr) " +
                    "FROM GlassCutRule gcr " +
                    "GROUP BY gcr.ruleType"
    )
    List<Object[]> countByRuleType();
}