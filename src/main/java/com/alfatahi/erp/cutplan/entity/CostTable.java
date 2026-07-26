package com.alfatahi.erp.cutplan.entity;

import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * COSTTABLE - Tabela de preços parametrizada para cálculo de custos
 *
 * Mantém histórico de preços de materiais e serviços por categoria
 * Permite versionamento e rastreabilidade de mudanças de preço
 *
 * Categorias:
 * - GLASS: Vidros (por tipo, espessura, cor)
 * - HARDWARE: Ferragens (dobradiças, puxadores, etc)
 * - ALUMINUM: Perfis de alumínio
 * - SILICONE: Silicones de vedação
 * - ACCESSORY: Acessórios
 *
 * Relacionamentos:
 * - 1:N com CostTableHistory
 * - N:1 com Supplier
 * - N:1 com AppUser (createdBy)
 */
@Entity
@Table(
        name = "cost_tables",
        indexes = {
                @Index(name = "idx_cost_tables_category", columnList = "category"),
                @Index(name = "idx_cost_tables_supplier_id", columnList = "supplier_id"),
                @Index(name = "idx_cost_tables_effective_from", columnList = "effective_from"),
                @Index(name = "idx_cost_tables_effective_to", columnList = "effective_to"),
                @Index(name = "idx_cost_tables_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cost_tables_category_item_type_effective",
                        columnNames = {"category", "item_type", "effective_from"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTable {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Categoria do item: GLASS, HARDWARE, ALUMINUM, SILICONE, ACCESSORY
     */
    @Column(nullable = false, length = 50)
    private String category;

    /**
     * Tipo/Descrição do item
     * Exemplos:
     * - Para GLASS: "TEMPERADO_TRANSPARENTE_8MM", "LAMINADO_FUMÊ_10MM"
     * - Para HARDWARE: "DOBRADI_ALA_LISA", "PUXADOR_CROMADO_100MM"
     * - Para ALUMINUM: "PERFIL_BOX_25MM", "TRILHO_JANELA"
     * - Para SILICONE: "SILICONE_BRANCO_600ML", "SILICONE_CINZA_600ML"
     */
    @Column(nullable = false, length = 255)
    private String itemType;

    /**
     * Descrição mais detalhada do item
     * Exemplo: "Vidro Temperado Transparente 8mm - Fornecimento por m²"
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Preço unitário (pode ser por m², por unidade, por metro linear, etc)
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Unidade de medida: M2 (metro quadrado), UNIT (unidade), M (metro), KG, etc
     */
    @Column(length = 20)
    private String unit;

    /**
     * Fornecedor associado (opcional)
     * Se null, é um preço padrão ou médio de mercado
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "supplier_id",
            foreignKey = @ForeignKey(name = "fk_cost_tables_supplier_id")
    )
    private Supplier supplier;

    /**
     * Data a partir da qual este preço é válido
     */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /**
     * Data até a qual este preço é válido (null = vigente indefinidamente até substituição)
     */
    private LocalDate effectiveTo;

    /**
     * Ativo/Inativo (soft delete)
     * Permite marcar preços como obsoletos sem deletar do histórico
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /**
     * Histórico de alterações de preço
     */
    @OneToMany(
            mappedBy = "costTable",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<CostTableHistory> history = new ArrayList<>();

    /**
     * Usuário que criou o registro
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_cost_tables_created_by")
    )
    private AppUser createdBy;

    /**
     * Data/hora de criação
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data/hora da última atualização
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Anotação opcional para campos de negócio
     */
    @Column(columnDefinition = "TEXT")
    private String remarks;

    /**
     * ========== MÉTODOS ==========
     */

    /**
     * Indica se este preço está vigente (ativa e dentro do período)
     */
    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return this.active &&
                !today.isBefore(this.effectiveFrom) &&
                (this.effectiveTo == null || !today.isAfter(this.effectiveTo));
    }

    /**
     * Indica se este preço está expirado
     */
    public boolean isExpired() {
        if (this.effectiveTo == null) return false;
        return LocalDate.now().isAfter(this.effectiveTo);
    }

    /**
     * Indica se este preço é válido para uma data específica
     */
    public boolean isValidAt(LocalDate date) {
        return this.active &&
                !date.isBefore(this.effectiveFrom) &&
                (this.effectiveTo == null || !date.isAfter(this.effectiveTo));
    }

    /**
     * Retorna a categoria em forma legível
     */
    public String getCategoryLabel() {
        return switch (this.category) {
            case "GLASS" -> "Vidro";
            case "HARDWARE" -> "Ferragem";
            case "ALUMINUM" -> "Alumínio";
            case "SILICONE" -> "Silicone";
            case "ACCESSORY" -> "Acessório";
            default -> this.category;
        };
    }

    /**
     * Retorna a unidade em forma legível
     */
    public String getUnitLabel() {
        return switch (this.unit) {
            case "M2" -> "m²";
            case "M" -> "m";
            case "UNIT" -> "un";
            case "KG" -> "kg";
            case "ML" -> "ml";
            default -> this.unit != null ? this.unit : "un";
        };
    }

    /**
     * Descrição formatada: "Vidro Temperado Transparente 8mm - R$ 150,00/m²"
     */
    public String getFormattedDescription() {
        String supplier = this.supplier != null ? " (" + this.supplier.getName() + ")" : "";
        return String.format(
                "%s - R$ %,.2f/%s%s",
                this.description != null ? this.description : this.itemType,
                this.unitPrice,
                getUnitLabel(),
                supplier
        );
    }

    /**
     * Adiciona um entry ao histórico
     */
    public void addHistory(CostTableHistory historyEntry) {
        this.history.add(historyEntry);
        historyEntry.setCostTable(this);
    }

    /**
     * Calcula a diferença percentual em relação ao último preço registrado
     */
    public BigDecimal getPercentageChange() {
        if (this.history.isEmpty() || this.history.size() < 2) {
            return BigDecimal.ZERO;
        }

        CostTableHistory previous = this.history.get(this.history.size() - 2);
        if (previous.getOldPrice() == null || previous.getOldPrice().equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        return this.unitPrice
                .subtract(previous.getOldPrice())
                .divide(previous.getOldPrice(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}