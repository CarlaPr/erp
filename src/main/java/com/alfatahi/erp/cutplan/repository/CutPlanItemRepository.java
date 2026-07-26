package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.CutPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CutPlanItemRepository - Acesso a dados de Itens do Plano de Corte
 *
 * Fornece queries especializadas para manipulação de itens individuais,
 * cálculos de custos e otimizações de layout
 */
@Repository
public interface CutPlanItemRepository extends JpaRepository<CutPlanItem, UUID> {

    /**
     * Busca todos os itens de um plano de corte específico
     */
    List<CutPlanItem> findByCutPlanId(UUID cutPlanId);

    /**
     * Busca itens com supplier para otimização
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "LEFT JOIN FETCH cpi.supplier " +
                    "WHERE cpi.cutPlan.id = :cutPlanId"
    )
    List<CutPlanItem> findByCutPlanIdWithSupplier(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens por tipo de vidro em um plano
     */
    List<CutPlanItem> findByCutPlanIdAndGlassType(UUID cutPlanId, String glassType);

    /**
     * Busca itens por espessura
     */
    List<CutPlanItem> findByCutPlanIdAndThickness(UUID cutPlanId, BigDecimal thickness);

    /**
     * Busca itens por acabamento
     */
    List<CutPlanItem> findByCutPlanIdAndFinishing(UUID cutPlanId, String finishing);

    /**
     * Busca itens não enviados ao fornecedor
     */
    List<CutPlanItem> findByCutPlanIdAndSentToSupplierFalse(UUID cutPlanId);

    /**
     * Busca itens já enviados ao fornecedor
     */
    List<CutPlanItem> findByCutPlanIdAndSentToSupplierTrue(UUID cutPlanId);

    /**
     * Soma a área total dos itens em um plano (para otimização)
     * Retorna a soma em mm²
     */
    @Query(
            "SELECT SUM(cpi.calculatedArea) FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId"
    )
    Optional<BigDecimal> sumAreaByCutPlanId(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Soma o custo total estimado dos itens
     */
    @Query(
            "SELECT SUM(cpi.estimatedCost) FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId"
    )
    Optional<BigDecimal> sumCostByCutPlanId(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Soma a quantidade total de peças
     */
    @Query(
            "SELECT SUM(cpi.quantity) FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId"
    )
    Optional<Integer> sumQuantityByCutPlanId(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Soma peso total estimado
     */
    @Query(
            "SELECT SUM(cpi.estimatedWeight) FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId"
    )
    Optional<BigDecimal> sumWeightByCutPlanId(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens maiores (para otimização de chapas - First Fit Decreasing)
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "ORDER BY cpi.calculatedArea DESC"
    )
    List<CutPlanItem> findByCutPlanIdOrderByAreaDesc(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens por tipo de vidro e espessura (para agrupamento em relatório)
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "  AND cpi.glassType = :glassType " +
                    "  AND cpi.thickness = :thickness " +
                    "ORDER BY cpi.finalWidth DESC, cpi.finalHeight DESC"
    )
    List<CutPlanItem> findByGlassTypeAndThickness(
            @Param("cutPlanId") UUID cutPlanId,
            @Param("glassType") String glassType,
            @Param("thickness") BigDecimal thickness
    );

    /**
     * Busca itens com fornecedor específico
     */
    List<CutPlanItem> findByCutPlanIdAndSupplierId(UUID cutPlanId, UUID supplierId);

    /**
     * Conta itens em um plano
     */
    long countByCutPlanId(UUID cutPlanId);

    /**
     * Busca itens com custo calculado (para verificação)
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "  AND cpi.estimatedCost > 0 " +
                    "ORDER BY cpi.estimatedCost DESC"
    )
    List<CutPlanItem> findByCutPlanIdWithCostCalculated(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens que necessitam revalidação (custo zero ou null)
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "  AND (cpi.estimatedCost IS NULL OR cpi.estimatedCost = 0) " +
                    "ORDER BY cpi.description ASC"
    )
    List<CutPlanItem> findByCutPlanIdNeedingCostRecalculation(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens com furos (para separação de processamento)
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "  AND cpi.drillingQuantity > 0"
    )
    List<CutPlanItem> findByCutPlanIdWithDrillings(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens com entalhes
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "  AND cpi.notchDescription IS NOT NULL"
    )
    List<CutPlanItem> findByCutPlanIdWithNotches(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca itens que excedem dimensões máximas da chapa padrão
     * Padrão: 3000x2250mm
     */
    @Query(
            "SELECT cpi FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "  AND (cpi.finalWidth > 3000 OR cpi.finalHeight > 2250)"
    )
    List<CutPlanItem> findByCutPlanIdExceedingStandardSheetSize(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Relatório: Agrupar itens por tipo de vidro
     */
    @Query(
            "SELECT cpi.glassType, COUNT(cpi), SUM(cpi.quantity), " +
                    "       SUM(cpi.calculatedArea), SUM(cpi.estimatedCost) " +
                    "FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "GROUP BY cpi.glassType " +
                    "ORDER BY SUM(cpi.estimatedCost) DESC"
    )
    List<Object[]> getGroupByGlassType(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Relatório: Agrupar itens por acabamento
     */
    @Query(
            "SELECT cpi.finishing, COUNT(cpi), SUM(cpi.quantity), SUM(cpi.estimatedCost) " +
                    "FROM CutPlanItem cpi " +
                    "WHERE cpi.cutPlan.id = :cutPlanId " +
                    "GROUP BY cpi.finishing " +
                    "ORDER BY cpi.finishing"
    )
    List<Object[]> getGroupByFinishing(@Param("cutPlanId") UUID cutPlanId);
}