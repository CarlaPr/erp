package com.alfatahi.erp.cutplan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * ============================================================================
 * GLASSFINISHING - Catálogo de acabamentos possíveis para vidro
 * ============================================================================
 * <p>
 * Define os tipos de acabamentos (bisote, lapidação, etc)
 * com seus respectivos custos de ajuste
 */
@Entity
@Table(
        name = "glass_finishings",
        indexes = {
                @Index(name = "idx_glass_finishings_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassFinishing {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Nome do acabamento (ex: "Bisote", "Lapidação", "Polimento")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Descrição detalhada
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Ajuste de custo (pode ser fixo ou percentual)
     * Exemplo: 50.00 (R$ 50 por m²) ou percentual
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal costAdjustment = BigDecimal.ZERO;

    /**
     * Tipo de ajuste: FIXED (valor fixo) ou PERCENTAGE (percentual)
     */
    @Column(length = 20)
    @Builder.Default
    private String adjustmentType = "FIXED";

    /**
     * Tempo adicional de processamento (em minutos)
     */
    private Integer processingTimeMinutes;

    /**
     * Ativo/Inativo
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
