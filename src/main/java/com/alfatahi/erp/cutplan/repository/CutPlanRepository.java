package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.CutPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CutPlanRepository - Acesso a dados de Plano de Corte
 *
 * Fornece operações CRUD e queries especializadas para busca
 * de planos de corte com filtros comuns
 */
@Repository
public interface CutPlanRepository extends JpaRepository<CutPlan, UUID> {

    /**
     * Busca o plano de corte de uma Ordem de Serviço específica
     */
    Optional<CutPlan> findByWorkOrderId(UUID workOrderId);

    /**
     * Busca todos os planos de um status específico
     */
    List<CutPlan> findByStatus(String status);

    /**
     * Busca com paginação por status
     */
    Page<CutPlan> findByStatus(String status, Pageable pageable);

    /**
     * Busca planos criados por um usuário específico
     */
    List<CutPlan> findByCreatedById(UUID userId);

    /**
     * Busca planos criados dentro de um período
     */
    @Query(
            "SELECT cp FROM CutPlan cp " +
                    "WHERE cp.createdAt BETWEEN :startDate AND :endDate " +
                    "ORDER BY cp.createdAt DESC"
    )
    List<CutPlan> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Busca planos com múltiplos status
     */
    List<CutPlan> findByStatusIn(List<String> statuses);

    /**
     * Conta planos por status
     */
    long countByStatus(String status);

    /**
     * Busca planos que ainda não foram enviados ao fornecedor
     */
    @Query(
            "SELECT cp FROM CutPlan cp " +
                    "WHERE cp.status IN ('DRAFT', 'APPROVED') " +
                    "ORDER BY cp.createdAt DESC"
    )
    List<CutPlan> findPendingPlans();

    /**
     * Busca planos com JOIN para otimização (evitar N+1)
     */
    @Query(
            "SELECT DISTINCT cp FROM CutPlan cp " +
                    "LEFT JOIN FETCH cp.items " +
                    "LEFT JOIN FETCH cp.workOrder " +
                    "WHERE cp.id = :id"
    )
    Optional<CutPlan> findByIdWithItems(@Param("id") UUID id);

    /**
     * Busca planos de uma OS com todos os detalhes
     */
    @Query(
            "SELECT DISTINCT cp FROM CutPlan cp " +
                    "LEFT JOIN FETCH cp.items cpi " +
                    "LEFT JOIN FETCH cp.history cph " +
                    "WHERE cp.workOrder.id = :workOrderId"
    )
    Optional<CutPlan> findByWorkOrderIdWithDetails(@Param("workOrderId") UUID workOrderId);

    /**
     * Busca planos aprovados que precisam ser enviados ao fornecedor
     */
    @Query(
            "SELECT cp FROM CutPlan cp " +
                    "WHERE cp.status = 'APPROVED' " +
                    "ORDER BY cp.updatedAt ASC"
    )
    List<CutPlan> findApprovedNotSent();

    /**
     * Estatísticas: Total de planos por status
     */
    @Query(
            "SELECT cp.status, COUNT(cp) FROM CutPlan cp " +
                    "GROUP BY cp.status"
    )
    List<Object[]> countByStatusGrouped();

    /**
     * Verifica se existe plano para uma OS
     */
    boolean existsByWorkOrderId(UUID workOrderId);

    /**
     * Busca com filtro avançado (combinado)
     */
    @Query(
            "SELECT cp FROM CutPlan cp " +
                    "WHERE (:status IS NULL OR cp.status = :status) " +
                    "  AND (:userId IS NULL OR cp.createdBy.id = :userId) " +
                    "  AND (cp.createdAt BETWEEN :startDate AND :endDate) " +
                    "ORDER BY cp.createdAt DESC"
    )
    Page<CutPlan> findWithFilters(
            @Param("status") String status,
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}