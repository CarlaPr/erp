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
 * GLASSNOTCH - Catálogo de tipos de entalhes (rebaixos)
 * ============================================================================
 * <p>
 * Define especificações para entalhes/rebaixos em vidro
 */
@Entity
@Table(
        name = "glass_notches",
        indexes = {
                @Index(name = "idx_glass_notches_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassNotch {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Nome do tipo de entalhe (ex: "Entalhe em U", "Entalhe em V", "Rebaixo Reto")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Descrição com especificações técnicas
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Largura do entalhe em milímetros
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal width;

    /**
     * Profundidade do entalhe em milímetros
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal depth;

    /**
     * Formato: U_SHAPE, V_SHAPE, RECTANGULAR, CUSTOM
     */
    @Column(length = 50)
    private String shape;

    /**
     * Custo por entalhe/rebaixo
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal cost;

    /**
     * Tempo de processamento (em minutos)
     */
    private Integer processingTime;

    /**
     * Comprimento mínimo do entalhe (em mm)
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal minLength;

    /**
     * Comprimento máximo do entalhe (em mm)
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal maxLength;

    /**
     * Ativo/Inativo
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
