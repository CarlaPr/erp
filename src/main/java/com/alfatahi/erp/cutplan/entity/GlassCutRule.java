package com.alfatahi.erp.cutplan.entity;

import com.alfatahi.erp.entity.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GLASSCUTRULE - Entidade que define regras técnicas parametrizadas
 * para corte de vidro por categoria de serviço
 *
 * Permite que as regras de desconto e folga sejam configuráveis na BD
 * sem necessidade de alteração de código.
 *
 * Exemplos de regras:
 * - BOX: LATERAL_DISCOUNT(30mm), SUPERIOR_DISCOUNT(20mm), HARDWARE_GAP(5mm)
 * - WINDOW: PROFILE_DISCOUNT(25mm), TRACK_GAP(3mm), ROLLER_GAP(2mm)
 * - MIRROR: POLISH_GAP(15mm), BEVEL_GAP(10mm), LED_SPACE(5mm)
 * - GUARDRAIL: SUPPORT_GAP(10mm), BUTTON_GAP(8mm), PROFILE_GAP(5mm)
 *
 * Relacionamentos:
 * - N:1 com ServiceCategory
 */
@Entity
@Table(
        name = "glass_cut_rules",
        indexes = {
                @Index(name = "idx_glass_cut_rules_service_category_id", columnList = "service_category_id"),
                @Index(name = "idx_glass_cut_rules_rule_type", columnList = "rule_type"),
                @Index(name = "idx_glass_cut_rules_parameter_name", columnList = "parameter_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_glass_cut_rules_category_parameter",
                        columnNames = {"service_category_id", "parameter_name"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassCutRule {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Categoria de serviço à qual esta regra se aplica
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "service_category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_glass_cut_rules_service_category_id")
    )
    private ServiceCategory serviceCategory;

    /**
     * Tipo de regra: DISCOUNT, GAP, ADJUSTMENT
     * - DISCOUNT: desconta valor da dimensão (reduz tamanho)
     * - GAP: folga que não se desconra da dimensão (apenas informativa)
     * - ADJUSTMENT: ajuste percentual
     */
    @Column(nullable = false, length = 50)
    private String ruleType;

    /**
     * Nome do parâmetro (chave única com serviceCategory)
     * Valores padronizados:
     * - LATERAL_DISCOUNT: Desconto lateral (horizontal)
     * - SUPERIOR_DISCOUNT: Desconto superior (vertical)
     * - INFERIOR_DISCOUNT: Desconto inferior
     * - HARDWARE_GAP: Folga para ferragens
     * - SILICONE_GAP: Folga para silicone
     * - INSTALL_GAP: Folga de instalação
     * - PROFILE_DISCOUNT: Desconto para perfis (janelas)
     * - TRACK_GAP: Folga de trilho
     * - ROLLER_GAP: Folga de roldana
     * - SEAL_GAP: Folga de vedação
     * - POLISH_GAP: Folga para polimento (espelhos)
     * - BEVEL_GAP: Folga para biselado
     * - LED_SPACE: Espaço para LED (espelhos)
     * - SUPPORT_GAP: Folga de suporte (guarda-corpo)
     * - BUTTON_GAP: Folga de botão
     */
    @Column(nullable = false, length = 100)
    private String parameterName;

    /**
     * Valor da regra (em unidade especificada)
     * Ex: 30 (para 30mm de desconto)
     *     5 (para 5% de ajuste)
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal value;

    /**
     * Unidade de medida: MM (milímetros), PERCENT (percentual), PCT (percentual)
     */
    @Column(nullable = false, length = 20)
    private String unit;

    /**
     * Descrição legível da regra
     * Ex: "Desconto lateral de 30mm para vidros em caixa (BOX)"
     *     "Folga de ferragem: 5mm em cada lado"
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Ativo/Inativo (soft delete)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Ordem de aplicação das regras (1 = primeira)
     * Importante quando há várias regras que se aplicam
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer applicationOrder = 0;

    /**
     * ========== MÉTODOS ==========
     */

    /**
     * Aplica a regra a um valor numérico
     *
     * @param originalValue Valor original (dimensão em mm)
     * @return Valor após aplicação da regra
     */
    public BigDecimal apply(BigDecimal originalValue) {
        if (!this.active || originalValue == null) {
            return originalValue;
        }

        if ("MM".equals(this.unit)) {
            // Desconto em milímetros: subtrai diretamente
            return originalValue.subtract(this.value);
        } else if ("PERCENT".equals(this.unit) || "PCT".equals(this.unit)) {
            // Desconto percentual: aplica percentual
            BigDecimal percentage = this.value.divide(
                    new BigDecimal("100"),
                    6,
                    java.math.RoundingMode.HALF_UP
            );
            return originalValue.multiply(percentage);
        }

        return originalValue;
    }

    /**
     * Retorna a unidade de forma legível
     */
    public String getUnitLabel() {
        return "MM".equals(this.unit) ? "mm" : "%";
    }

    /**
     * Retorna o tipo de regra em forma legível
     */
    public String getRuleTypeLabel() {
        return switch (this.ruleType) {
            case "DISCOUNT" -> "Desconto";
            case "GAP" -> "Folga";
            case "ADJUSTMENT" -> "Ajuste";
            default -> this.ruleType;
        };
    }

    /**
     * Retorna o nome do parâmetro em forma legível
     */
    public String getParameterNameLabel() {
        return switch (this.parameterName) {
            case "LATERAL_DISCOUNT" -> "Desconto Lateral";
            case "SUPERIOR_DISCOUNT" -> "Desconto Superior";
            case "INFERIOR_DISCOUNT" -> "Desconto Inferior";
            case "HARDWARE_GAP" -> "Folga de Ferragem";
            case "SILICONE_GAP" -> "Folga de Silicone";
            case "INSTALL_GAP" -> "Folga de Instalação";
            case "PROFILE_DISCOUNT" -> "Desconto de Perfil";
            case "TRACK_GAP" -> "Folga de Trilho";
            case "ROLLER_GAP" -> "Folga de Roldana";
            case "SEAL_GAP" -> "Folga de Vedação";
            case "POLISH_GAP" -> "Folga de Polimento";
            case "BEVEL_GAP" -> "Folga de Biselado";
            case "LED_SPACE" -> "Espaço para LED";
            case "SUPPORT_GAP" -> "Folga de Suporte";
            case "BUTTON_GAP" -> "Folga de Botão";
            default -> this.parameterName;
        };
    }

    /**
     * Retorna descrição formatada: "Desconto Lateral: 30mm (desconto de laterais)"
     */
    public String getFormattedDescription() {
        return String.format(
                "%s: %s%s (%s)",
                getParameterNameLabel(),
                this.value,
                getUnitLabel(),
                this.description != null ? this.description : getRuleTypeLabel()
        );
    }
}