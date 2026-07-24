package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.GlassFinishing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================================
 * GlassFinishingRepository
 * ============================================================================
 * <p>
 * Fornece acesso ao catálogo de acabamentos de vidro
 */
@Repository
public interface GlassFinishingRepository extends JpaRepository<GlassFinishing, UUID> {

    /**
     * Busca acabamento por nome
     */
    Optional<GlassFinishing> findByName(String name);

    /**
     * Busca todos os acabamentos ativos
     */
    List<GlassFinishing> findByActiveTrue();

    /**
     * Busca todos os acabamentos (incluindo inativos)
     */
    List<GlassFinishing> findAll();

    /**
     * Busca acabamentos por palavra-chave
     */
    @Query(
            "SELECT gf FROM GlassFinishing gf " +
                    "WHERE LOWER(gf.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  AND gf.active = true"
    )
    List<GlassFinishing> searchByName(@Param("search") String search);

    /**
     * Conta acabamentos ativos
     */
    long countByActiveTrue();

    /**
     * Busca acabamentos por tipo de ajuste
     */
    List<GlassFinishing> findByAdjustmentType(String adjustmentType);

    /**
     * Busca acabamentos ordenados por custo (mais caros primeiro)
     */
    @Query(
            "SELECT gf FROM GlassFinishing gf " +
                    "WHERE gf.active = true " +
                    "ORDER BY gf.costAdjustment DESC"
    )
    List<GlassFinishing> findAllOrderedByCost();
}
