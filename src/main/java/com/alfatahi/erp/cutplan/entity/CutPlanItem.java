package com.alfatahi.erp.cutplan.entity;


import com.alfatahi.erp.entity.Supplier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * CUTPLANITEM - Entidade que representa um item (peça) no Plano de Corte
 *
 * Cada item contém:
 * - Especificações de vidro (tipo, espessura, cor, acabamento)
 * - Dimensões brutas e finais (após aplicação de regras técnicas)
 * - Quantidade e cálculos de área
 * - Custos estimados (vidro, ferragens, alumínio, silicone)
 * - Especificações adicionais (furos, entalhes, acabamentos)
 *
 * Relacionamentos:
 * - N:1 com CutPlan
 * - N:1 com Supplier
 */
@Entity
@Table(
        name = "cut_plan_items",
        indexes = {
                @Index(name = "idx_cut_plan_items_cut_plan_id", columnList = "cut_plan_id"),
                @Index(name = "idx_cut_plan_items_supplier_id", columnList = "supplier_id"),
                @Index(name = "idx_cut_plan_items_glass_type", columnList = "glass_type")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanItem {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Referência para o Plano de Corte pai
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cut_plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cut_plan_items_cut_plan_id")
    )
    private CutPlan cutPlan;

    /**
     * Descrição comercial do item (ex: "Vidro Temperado Transparente 8mm - Sala de Estar")
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Ambiente onde será instalado (ex: "Sala", "Cozinha", "Quarto")
     */
    @Column(length = 100)
    private String environment;

    /**
     * ========== ESPECIFICAÇÕES DE VIDRO ==========
     */

    /**
     * Tipo de vidro: COMUM, TEMPERADO, LAMINADO, ESPELHADO
     */
    @Column(nullable = false, length = 50)
    private String glassType;

    /**
     * Espessura do vidro em milímetros (ex: 4, 6, 8, 10, 12)
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal thickness;

    /**
     * Cor do vidro: TRANSPARENTE, FUMÊ, VERDE, AZUL, ROSA, BRONZE
     */
    @Column(length = 50)
    private String color;

    /**
     * ========== ACABAMENTO E PROCESSAMENTO ==========
     */

    /**
     * Acabamento: SEM_ACABAMENTO, BISOTE, LAPIDACAO, POLIMENTO
     */
    @Column(length = 50)
    private String finishing;

    /**
     * ========== DIMENSÕES BRUTAS (do projeto) ==========
     * Em milímetros, sem aplicação de regras técnicas
     */

    /**
     * Largura bruta (antes das regras)
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal grossWidth;

    /**
     * Altura bruta (antes das regras)
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal grossHeight;

    /**
     * ========== DIMENSÕES FINAIS (após regras técnicas) ==========
     * Em milímetros, já com descontos laterais, folgas, etc
     */

    /**
     * Largura final (após aplicação de regras)
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal finalWidth;

    /**
     * Altura final (após aplicação de regras)
     */
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal finalHeight;

    /**
     * ========== QUANTIDADE E ÁREA ==========
     */

    /**
     * Quantidade de peças deste tamanho
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Área calculada em mm² (para otimização de chapas)
     * Calculada como: finalWidth × finalHeight × quantity
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal calculatedArea;

    /**
     * Peso estimado em kg
     * Fórmula: área(m²) × espessura(mm) × densidade(kg/m³) × quantidade
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedWeight;

    /**
     * ========== CUSTOS ESTIMADOS ==========
     */

    /**
     * Custo do vidro (base + ajustes por tipo e acabamento)
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal glassCost = BigDecimal.ZERO;

    /**
     * Custo total de ferragens (dobradiças, puxadores, etc)
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal hardwaresTotalCost = BigDecimal.ZERO;

    /**
     * Custo de alumínio/perfis (se aplicável)
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal aluminumTotalCost = BigDecimal.ZERO;

    /**
     * Custo de silicone (vedação)
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal siliconeTotalCost = BigDecimal.ZERO;

    /**
     * Custo total estimado = vidro + ferragens + alumínio + silicone
     */
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    /**
     * ========== FORNECEDOR ==========
     */

    /**
     * Fornecedor sugerido para este item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "supplier_id",
            foreignKey = @ForeignKey(name = "fk_cut_plan_items_supplier_id")
    )
    private Supplier supplier;

    /**
     * ========== ESPECIFICAÇÕES ADICIONAIS ==========
     */

    /**
     * Observações/notas adicionais
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Ângulo para corte (em graus, se aplicável)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal angle;

    /**
     * ========== FUROS (DRILLS) ==========
     */

    /**
     * Diâmetro dos furos em milímetros
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal drillingDiameter;

    /**
     * Quantidade de furos
     */
    private Integer drillingQuantity;

    /**
     * Custo unitário por furo
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal drillingCostPerUnit;

    /**
     * ========== ENTALHES (NOTCHES) ==========
     */

    /**
     * Descrição do tipo de entalhe
     */
    @Column(length = 200)
    private String notchDescription;

    /**
     * Custo total de entalhes
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal notchCost;

    /**
     * ========== CONTROLE ==========
     */

    /**
     * Indica se este item foi enviado ao fornecedor
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean sentToSupplier = false;

    /**
     * Observações da resposta do fornecedor
     */
    @Column(columnDefinition = "TEXT")
    private String supplierFeedback;

    /**
     * ========== MÉTODOS DE NEGÓCIO ==========
     */

    /**
     * Calcula a área em m² para referência
     */
    public BigDecimal getAreaInSquareMeters() {
        if (this.finalWidth == null || this.finalHeight == null) {
            return BigDecimal.ZERO;
        }
        // Converter de mm² para m² (dividir por 1.000.000)
        return this.calculatedArea.divide(
                new BigDecimal("1000000"),
                6,
                java.math.RoundingMode.HALF_UP
        );
    }

    /**
     * Retorna o custo total (para fácil acesso)
     */
    public BigDecimal getTotalCost() {
        return this.estimatedCost;
    }

    /**
     * Retorna a descrição resumida
     */
    public String getShortDescription() {
        return String.format(
                "%s | %s | %sx%s mm | Qtd: %d",
                this.glassType,
                this.thickness,
                this.finalWidth,
                this.finalHeight,
                this.quantity
        );
    }
}