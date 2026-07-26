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
 * GLASSDRILLING - Catálogo de tipos de furos (furações)
 * ============================================================================
 * <p>
 * Define especificações padrão para furos em vidro
 */
@Entity
@Table(
        name = "glass_drillings",
        indexes = {
                @Index(name = "idx_glass_drillings_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassDrilling {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Nome descritivo do tipo de furo (ex: "Furo 8mm", "Furo 10mm com Rebaixa")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Descrição detalhada
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Diâmetro do furo em milímetros
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal diameter;

    /**
     * Profundidade de rebaixa (se aplicável)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal rebaixDepth;

    /**
     * Custo por unidade (por furo)
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal costPerUnit;

    /**
     * Tempo de processamento por furo (em minutos)
     */
    private Integer timePerHole;

    /**
     * Ativo/Inativo
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
