package com.alfatahi.erp.cutplan.entity;

import com.alfatahi.erp.entity.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.*;

/**
 * ============================================================================
 * GLASSTEMPLATE - Template configurável para especificações de vidro
 * ============================================================================
 *
 * Armazena templates de vidro reutilizáveis por categoria
 * Facilitando a criação de itens de corte com especificações padronizadas
 */
@Entity
@Table(
        name = "glass_templates",
        indexes = {
                @Index(name = "idx_glass_templates_service_category_id", columnList = "service_category_id"),
                @Index(name = "idx_glass_templates_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassTemplate {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Categoria de serviço (BOX, JANELA, ESPELHO, GUARDA-CORPO, etc)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "service_category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_glass_templates_service_category_id")
    )
    private ServiceCategory serviceCategory;

    /**
     * Nome do template (ex: "Box Padrão 8mm", "Janela Dupla Vidro")
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Descrição do template
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Tipo de vidro padrão do template
     */
    @Column(length = 50)
    private String defaultGlassType;

    /**
     * Espessura padrão
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal defaultThickness;

    /**
     * Cor padrão
     */
    @Column(length = 50)
    private String defaultColor;

    /**
     * Acabamentos aplicáveis neste template
     * JSON array: ["BISOTE", "POLIMENTO", "LAPIDACAO"]
     */
    @Column(columnDefinition = "TEXT")
    private String applicableFinishings;

    /**
     * Tipos de furos aplicáveis
     * JSON array de IDs de GlassDrilling
     */
    @Column(columnDefinition = "TEXT")
    private String applicableDrillings;

    /**
     * Tipos de entalhes aplicáveis
     * JSON array de IDs de GlassNotch
     */
    @Column(columnDefinition = "TEXT")
    private String applicableNotches;

    /**
     * Ativo/Inativo
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}

