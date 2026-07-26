package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.CostTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CostTableRepository - Acesso a dados de Tabelas de Preço
 *
 * Fornece queries especializadas para busca de preços vigentes,
 * histórico de preços, e gerenciamento de versões
 */
@Repository
public interface CostTableRepository extends JpaRepository<CostTable, UUID> {

    /**
     * Busca todas as entradas de uma categoria
     */
    List<CostTable> findByCategory(String category);

    /**
     * Busca com paginação por categoria
     */
    Page<CostTable> findByCategory(String category, Pageable pageable);

    /**
     * Busca apenas preços ativos de uma categoria
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.active = true"
    )
    List<CostTable> findActiveByCategoryAndActiveTrue(@Param("category") String category);

    /**
     * Busca preços vigentes (ativos e dentro do período de validade) de uma categoria
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.active = true " +
                    "  AND CURRENT_DATE >= ct.effectiveFrom " +
                    "  AND (ct.effectiveTo IS NULL OR CURRENT_DATE <= ct.effectiveTo) " +
                    "ORDER BY ct.createdAt DESC"
    )
    List<CostTable> findCurrentByCategory(@Param("category") String category);

    /**
     * Busca o preço vigente mais recente para um item específico
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.itemType = :itemType " +
                    "  AND ct.active = true " +
                    "  AND CURRENT_DATE >= ct.effectiveFrom " +
                    "  AND (ct.effectiveTo IS NULL OR CURRENT_DATE <= ct.effectiveTo) " +
                    "ORDER BY ct.createdAt DESC LIMIT 1"
    )
    Optional<CostTable> findCurrentPrice(
            @Param("category") String category,
            @Param("itemType") String itemType
    );

    /**
     * Busca preços para uma data específica (histórico)
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.itemType = :itemType " +
                    "  AND ct.active = true " +
                    "  AND :targetDate >= ct.effectiveFrom " +
                    "  AND (ct.effectiveTo IS NULL OR :targetDate <= ct.effectiveTo)"
    )
    Optional<CostTable> findPriceAtDate(
            @Param("category") String category,
            @Param("itemType") String itemType,
            @Param("targetDate") LocalDate targetDate
    );

    /**
     * Busca preços de um fornecedor específico
     */
    List<CostTable> findBySupplierIdAndActiveTrue(UUID supplierId);

    /**
     * Busca preços ativos de um fornecedor em uma categoria
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.supplier.id = :supplierId " +
                    "  AND ct.category = :category " +
                    "  AND ct.active = true " +
                    "ORDER BY ct.createdAt DESC"
    )
    List<CostTable> findBySupplierIdAndCategory(
            @Param("supplierId") UUID supplierId,
            @Param("category") String category
    );

    /**
     * Busca histórico de preços para um item (todas as versões)
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.itemType = :itemType " +
                    "ORDER BY ct.effectiveFrom DESC"
    )
    List<CostTable> findPriceHistory(
            @Param("category") String category,
            @Param("itemType") String itemType
    );

    /**
     * Busca preços expirados (para arquivamento)
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.effectiveTo IS NOT NULL " +
                    "  AND ct.effectiveTo < CURRENT_DATE " +
                    "ORDER BY ct.effectiveTo DESC"
    )
    List<CostTable> findExpiredPrices();

    /**
     * Busca preços que expiram em breve (próximos 7 dias)
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.effectiveTo IS NOT NULL " +
                    "  AND ct.effectiveTo >= CURRENT_DATE " +
                    "  AND ct.effectiveTo <= CURRENT_DATE + 7 " +
                    "ORDER BY ct.effectiveTo ASC"
    )
    List<CostTable> findSoonToExpire();

    /**
     * Conta entradas por categoria
     */
    long countByCategory(String category);

    /**
     * Conta entradas ativas por categoria
     */
    @Query(
            "SELECT COUNT(ct) FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.active = true"
    )
    long countActiveByCategory(@Param("category") String category);

    /**
     * Verifica se existe preço para um item
     */
    boolean existsByPrimaryKey(String category, String itemType);

    /**
     * Busca por tipo de item (parcial) - útil para autocomplete
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND LOWER(ct.itemType) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  AND ct.active = true " +
                    "ORDER BY ct.itemType ASC"
    )
    List<CostTable> findByItemTypeContaining(
            @Param("category") String category,
            @Param("search") String search
    );

    /**
     * Busca com filtros avançados
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE (:category IS NULL OR ct.category = :category) " +
                    "  AND (:itemType IS NULL OR LOWER(ct.itemType) LIKE LOWER(CONCAT('%', :itemType, '%'))) " +
                    "  AND (:supplierId IS NULL OR ct.supplier.id = :supplierId) " +
                    "  AND ct.active = true " +
                    "ORDER BY ct.category ASC, ct.itemType ASC"
    )
    Page<CostTable> findWithFilters(
            @Param("category") String category,
            @Param("itemType") String itemType,
            @Param("supplierId") UUID supplierId,
            Pageable pageable
    );

    /**
     * Calcula preço médio por categoria
     */
    @Query(
            "SELECT ct.category, AVG(ct.unitPrice), MIN(ct.unitPrice), MAX(ct.unitPrice) " +
                    "FROM CostTable ct " +
                    "WHERE ct.active = true " +
                    "GROUP BY ct.category"
    )
    List<Object[]> getAveragePriceByCategory();

    /**
     * Calcula preço médio por fornecedor
     */
    @Query(
            "SELECT ct.supplier.name, AVG(ct.unitPrice), COUNT(ct) " +
                    "FROM CostTable ct " +
                    "WHERE ct.supplier IS NOT NULL " +
                    "  AND ct.active = true " +
                    "GROUP BY ct.supplier.id, ct.supplier.name " +
                    "ORDER BY AVG(ct.unitPrice) ASC"
    )
    List<Object[]> getAveragePriceBySupplier();

    /**
     * Busca variações de preço (itens que tiveram mudança recente)
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE ct.category = :category " +
                    "  AND ct.createdAt >= :fromDate " +
                    "ORDER BY ct.createdAt DESC"
    )
    List<CostTable> findRecentChanges(
            @Param("category") String category,
            @Param("fromDate") LocalDate fromDate
    );

    /**
     * Relatório: Produtos sem preço vigente (null ou inativo)
     */
    @Query(
            "SELECT ct FROM CostTable ct " +
                    "WHERE (ct.active = false OR ct.effectiveTo < CURRENT_DATE) " +
                    "ORDER BY ct.category ASC, ct.itemType ASC"
    )
    List<CostTable> findOutdatedPrices();

    /**
     * Busca preços duplicados (mesmo item, mesma categoria)
     */
    @Query(
            "SELECT ct.category, ct.itemType, COUNT(ct), " +
                    "       MIN(ct.unitPrice), MAX(ct.unitPrice) " +
                    "FROM CostTable ct " +
                    "WHERE ct.active = true " +
                    "GROUP BY ct.category, ct.itemType " +
                    "HAVING COUNT(ct) > 1"
    )
    List<Object[]> findDuplicateItems();
}