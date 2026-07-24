package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================================
 * GlassTemplateRepository
 * ============================================================================
 *
 * Fornece acesso a templates de vidro reutilizáveis por categoria
 */
@Repository
public interface GlassTemplateRepository extends JpaRepository<GlassTemplate, UUID> {

    /**
     * Busca templates para uma categoria específica
     */
    List<GlassTemplate> findByServiceCategoryId(UUID categoryId);

    /**
     * Busca templates ativos de uma categoria
     */
    @Query(
            "SELECT gt FROM GlassTemplate gt " +
                    "WHERE gt.serviceCategory.id = :categoryId " +
                    "  AND gt.active = true " +
                    "ORDER BY gt.name ASC"
    )
    List<GlassTemplate> findActiveByServiceCategoryId(@Param("categoryId") UUID categoryId);

    /**
     * Busca template por nome (único)
     */
    Optional<GlassTemplate> findByName(String name);

    /**
     * Busca template por nome com categoria
     */
    Optional<GlassTemplate> findByNameAndServiceCategoryId(String name, UUID categoryId);

    /**
     * Conta templates por categoria
     */
    long countByServiceCategoryId(UUID categoryId);

    /**
     * Busca templates com palavra-chave no nome
     */
    @Query(
            "SELECT gt FROM GlassTemplate gt " +
                    "WHERE gt.serviceCategory.id = :categoryId " +
                    "  AND LOWER(gt.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  AND gt.active = true"
    )
    List<GlassTemplate> searchByName(@Param("categoryId") UUID categoryId, @Param("search") String search);
}

