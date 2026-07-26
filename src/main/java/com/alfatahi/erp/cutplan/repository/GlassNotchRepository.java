package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.GlassNotch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ============================================================================
 * GlassNotchRepository
 * ============================================================================
 * <p>
 * Fornece acesso ao catálogo de tipos de entalhes
 */
@Repository
public interface GlassNotchRepository extends JpaRepository<GlassNotch, UUID> {

    /**
     * Busca entalhe por nome
     */
    Optional<GlassNotch> findByName(String name);

    /**
     * Busca todos os entalhes ativos
     */
    List<GlassNotch> findByActiveTrue();

    /**
     * Busca entalhes por formato
     */
    List<GlassNotch> findByShape(String shape);

    /**
     * Busca entalhes ativos por formato
     */
    @Query(
            "SELECT gn FROM GlassNotch gn " +
                    "WHERE gn.shape = :shape " +
                    "  AND gn.active = true"
    )
    List<GlassNotch> findByShapeActive(@Param("shape") String shape);

    /**
     * Busca entalhes por comprimento (dentro do intervalo mín-máx)
     */
    @Query(
            "SELECT gn FROM GlassNotch gn " +
                    "WHERE gn.minLength <= :length " +
                    "  AND gn.maxLength >= :length " +
                    "  AND gn.active = true"
    )
    List<GlassNotch> findByLengthRange(@Param("length") java.math.BigDecimal length);

    /**
     * Busca entalhes por palavra-chave
     */
    @Query(
            "SELECT gn FROM GlassNotch gn " +
                    "WHERE LOWER(gn.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "  AND gn.active = true"
    )
    List<GlassNotch> searchByName(@Param("search") String search);

    /**
     * Conta entalhes ativos
     */
    long countByActiveTrue();

    /**
     * Busca entalhes ordenados por custo
     */
    @Query(
            "SELECT gn FROM GlassNotch gn " +
                    "WHERE gn.active = true " +
                    "ORDER BY gn.cost DESC"
    )
    List<GlassNotch> findAllOrderedByCost();

    /**
     * Busca entalhes com tempo de processamento
     */
    @Query(
            "SELECT gn FROM GlassNotch gn " +
                    "WHERE gn.processingTime IS NOT NULL " +
                    "  AND gn.active = true " +
                    "ORDER BY gn.processingTime ASC"
    )
    List<GlassNotch> findWithProcessingTime();

    /**
     * Relatório: Formatos disponíveis
     */
    @Query(
            "SELECT DISTINCT gn.shape FROM GlassNotch gn " +
                    "WHERE gn.active = true"
    )
    List<String> findDistinctShapes();

    /**
     * Relatório: Entalhes por formato
     */
    @Query(
            "SELECT gn.shape, COUNT(gn) FROM GlassNotch gn " +
                    "WHERE gn.active = true " +
                    "GROUP BY gn.shape"
    )
    List<Object[]> countByShape();
}
