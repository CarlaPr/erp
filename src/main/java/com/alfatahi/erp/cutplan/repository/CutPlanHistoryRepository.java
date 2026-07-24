package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.CutPlanHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CutPlanHistoryRepository - Auditoria e rastreabilidade de alterações
 *
 * Fornece acesso ao histórico de mudanças em planos de corte
 * para auditoria, rastreabilidade e análise
 */
@Repository
public interface CutPlanHistoryRepository extends JpaRepository<CutPlanHistory, UUID> {

    /**
     * Busca histórico completo de um plano (ordenado cronologicamente)
     */
    List<CutPlanHistory> findByCutPlanIdOrderByChangedAtDesc(UUID cutPlanId);

    /**
     * Busca histórico com paginação
     */
    Page<CutPlanHistory> findByCutPlanIdOrderByChangedAtDesc(UUID cutPlanId, Pageable pageable);

    /**
     * Busca histórico de um tipo específico de mudança
     */
    List<CutPlanHistory> findByCutPlanIdAndChangeTypeOrderByChangedAtDesc(
            UUID cutPlanId,
            String changeType
    );

    /**
     * Busca alterações feitas por um usuário específico
     */
    List<CutPlanHistory> findByChangedByIdOrderByChangedAtDesc(UUID userId);

    /**
     * Busca alterações dentro de um período
     */
    List<CutPlanHistory> findByChangedAtBetweenOrderByChangedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Busca alterações de um plano dentro de um período
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "  AND cph.changedAt BETWEEN :startDate AND :endDate " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findByCutPlanIdAndDateRange(
            @Param("cutPlanId") UUID cutPlanId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca histórico recente (últimos N dias)
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "  AND cph.changedAt >= :fromDate " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findRecentByCutPlanId(
            @Param("cutPlanId") UUID cutPlanId,
            @Param("fromDate") LocalDateTime fromDate
    );

    /**
     * Conta históricos de um plano
     */
    long countByCutPlanId(UUID cutPlanId);

    /**
     * Busca a última alteração de um plano
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "ORDER BY cph.changedAt DESC LIMIT 1"
    )
    CutPlanHistory findLatestByCutPlanId(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca histórico de mudanças de status específicos
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "  AND cph.changeType IN ('STATUS_CHANGED', 'APPROVED', 'SENT_TO_SUPPLIER') " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findStatusChanges(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca alterações de itens (ITEM_ADDED, ITEM_UPDATED, ITEM_REMOVED)
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "  AND cph.changeType LIKE 'ITEM_%' " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findItemChanges(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca alterações relacionadas a um item específico
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "  AND cph.affectedItemId = :itemId " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findByAffectedItemId(
            @Param("cutPlanId") UUID cutPlanId,
            @Param("itemId") UUID itemId
    );

    /**
     * Estatísticas: Contar mudanças por tipo
     */
    @Query(
            "SELECT cph.changeType, COUNT(cph) FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "GROUP BY cph.changeType"
    )
    List<Object[]> countByChangeType(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Estatísticas: Contar mudanças por usuário
     */
    @Query(
            "SELECT cph.changedBy.name, COUNT(cph) FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "GROUP BY cph.changedBy.id, cph.changedBy.name"
    )
    List<Object[]> countByUser(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Auditoria completa: Histórico desde criação
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "ORDER BY cph.version ASC, cph.changedAt ASC"
    )
    List<CutPlanHistory> getFullAuditTrail(@Param("cutPlanId") UUID cutPlanId);

    /**
     * Busca versões específicas para rollback (se necessário)
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.cutPlan.id = :cutPlanId " +
                    "  AND cph.version = :version " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findByVersion(
            @Param("cutPlanId") UUID cutPlanId,
            @Param("version") Integer version
    );

    /**
     * Busca histórico de aprovações
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.changeType = 'APPROVED' " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findAllApprovals();

    /**
     * Busca histórico de envios a fornecedores
     */
    @Query(
            "SELECT cph FROM CutPlanHistory cph " +
                    "WHERE cph.changeType = 'SENT_TO_SUPPLIER' " +
                    "ORDER BY cph.changedAt DESC"
    )
    List<CutPlanHistory> findAllSupplierSends();
}