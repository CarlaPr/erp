package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.GlassDrilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================================
 * GlassDrillingRepository
 * ============================================================================
 * <p>
 * Fornece acesso ao catálogo de tipos de furos
 */
@Repository
public interface GlassDrillingRepository extends JpaRepository<GlassDrilling, UUID> {

    /**
     * Busca furo por nome
     */
    Optional<GlassDrilling> findByName(String name);

    /**
     * Busca todos os furos ativos
     */
    List<GlassDrilling> findByActiveTrue();

    /**
     * Busca furo por diâmetro específico
     */
    @Query(
            "SELECT gd FROM GlassDrilling gd " +
                    "WHERE gd.diameter = :diameter " +
                    "  AND gd.active = true"
    )
    List<GlassDrilling> findByDiameter(@Param("diameter") java.math.BigDecimal diameter);

    /**
     * Busca furos por intervalo de diâmetro
     */
    @Query(
            "SELECT gd FROM GlassDrilling gd " +
                    "WHERE gd.diameter >= :minDiameter " +
                    "  AND gd.diameter <= :maxDiameter " +
                    "  AND gd.active = true " +
                    "ORDER BY gd.diameter ASC"
    )
    List<GlassDrilling> findByDiameterRange(
            @Param("minDiameter") java.math.BigDecimal minDiameter,
            @Param("maxDiameter") java.math.BigDecimal maxDiameter
    );

    /**
     * Busca furos com rebaixa
     */
    @Query(
            "SELECT gd FROM GlassDrilling gd " +
                    "WHERE gd.rebaixDepth IS NOT NULL " +
                    "  AND gd.rebaixDepth > 0 " +
                    "  AND gd.active = true"
    )
    List<GlassDrilling> findWithRebaixDepth();

    /**
     * Busca furos por palavra-chave
     */
    @Query(
            "SELECT gd FROM GlassDrilling gd " +
                    "WHERE LOWER(gd.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  AND gd.active = true"
    )
    List<GlassDrilling> searchByName(@Param("search") String search);

    /**
     * Conta furos ativos
     */
    long countByActiveTrue();

    /**
     * Busca furos ordenados por custo
     */
    @Query(
            "SELECT gd FROM GlassDrilling gd " +
                    "WHERE gd.active = true " +
                    "ORDER BY gd.costPerUnit DESC"
    )
    List<GlassDrilling> findAllOrderedByCost();
}
