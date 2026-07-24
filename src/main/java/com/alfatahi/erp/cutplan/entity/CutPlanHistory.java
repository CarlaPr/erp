package com.alfatahi.erp.cutplan.entity;

import com.alfatahi.erp.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CUTPLANHISTORY - Entidade para auditoria e rastreabilidade
 * Registra todas as alterações significativas no Plano de Corte
 *
 * Tipos de mudança:
 * - CREATED: Plano foi criado
 * - ITEM_ADDED: Item foi adicionado
 * - ITEM_UPDATED: Item foi modificado
 * - ITEM_REMOVED: Item foi removido
 * - RULES_APPLIED: Regras técnicas foram reaplicadas
 * - COSTS_RECALCULATED: Custos foram recalculados
 * - LAYOUT_OPTIMIZED: Layout foi otimizado
 * - STATUS_CHANGED: Status foi alterado (DRAFT -> APPROVED -> SENT)
 * - APPROVED: Plano foi aprovado
 * - SENT_TO_SUPPLIER: Plano foi enviado
 * - CANCELLED: Plano foi cancelado
 *
 * Relacionamentos:
 * - N:1 com CutPlan
 * - N:1 com AppUser (who changed)
 */
@Entity
@Table(
        name = "cut_plan_history",
        indexes = {
                @Index(name = "idx_cut_plan_history_cut_plan_id", columnList = "cut_plan_id"),
                @Index(name = "idx_cut_plan_history_changed_by", columnList = "changed_by"),
                @Index(name = "idx_cut_plan_history_changed_at", columnList = "changed_at"),
                @Index(name = "idx_cut_plan_history_change_type", columnList = "change_type")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanHistory {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Referência para o Plano de Corte
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cut_plan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cut_plan_history_cut_plan_id")
    )
    private CutPlan cutPlan;

    /**
     * Usuário que fez a alteração
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "changed_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cut_plan_history_changed_by")
    )
    private AppUser changedBy;

    /**
     * Tipo/categoria da mudança
     * Valores: CREATED, ITEM_ADDED, ITEM_UPDATED, ITEM_REMOVED,
     *          RULES_APPLIED, COSTS_RECALCULATED, LAYOUT_OPTIMIZED,
     *          STATUS_CHANGED, APPROVED, SENT_TO_SUPPLIER, CANCELLED
     */
    @Column(nullable = false, length = 50)
    private String changeType;

    /**
     * Descrição detalhada da alteração
     * Exemplo: "Adicionado item: Vidro Temperado 8mm 1500x1500"
     *          "Status alterado de DRAFT para APPROVED"
     *          "Custo recalculado: R$ 500.00 -> R$ 520.00"
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Versão do plano nesta alteração
     * Incrementado quando há mudanças significativas
     */
    @Column(nullable = false)
    private Integer version;

    /**
     * Data/hora da alteração
     */
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /**
     * ========== CAMPOS OPCIONAIS PARA AUDITORIA DETALHADA ==========
     */

    /**
     * Valores antigos (JSON serializado para comparação)
     * Exemplo: {"status": "DRAFT", "totalItems": 5}
     */
    @Column(columnDefinition = "TEXT")
    private String oldValues;

    /**
     * Valores novos (JSON serializado)
     * Exemplo: {"status": "APPROVED", "totalItems": 6}
     */
    @Column(columnDefinition = "TEXT")
    private String newValues;

    /**
     * ID do item afetado (se aplicável)
     * Usado quando a mudança é relacionada a um item específico
     */
    private UUID affectedItemId;

    /**
     * Referência do item afetado em texto (para clareza no relatório)
     */
    @Column(length = 500)
    private String affectedItemDescription;

    /**
     * ========== MÉTODOS ==========
     */

    /**
     * Indica se este é uma mudança de status
     */
    public boolean isStatusChange() {
        return "STATUS_CHANGED".equals(this.changeType);
    }

    /**
     * Indica se este é uma mudança de item
     */
    public boolean isItemChange() {
        return this.changeType.startsWith("ITEM_");
    }

    /**
     * Indica se este é uma aprovação
     */
    public boolean isApproval() {
        return "APPROVED".equals(this.changeType) ||
                "STATUS_CHANGED".equals(this.changeType);
    }

    /**
     * Retorna o tipo de mudança em formato legível
     */
    public String getChangeTypeLabel() {
        return switch (this.changeType) {
            case "CREATED" -> "Plano Criado";
            case "ITEM_ADDED" -> "Item Adicionado";
            case "ITEM_UPDATED" -> "Item Atualizado";
            case "ITEM_REMOVED" -> "Item Removido";
            case "RULES_APPLIED" -> "Regras Aplicadas";
            case "COSTS_RECALCULATED" -> "Custos Recalculados";
            case "LAYOUT_OPTIMIZED" -> "Layout Otimizado";
            case "STATUS_CHANGED" -> "Status Alterado";
            case "APPROVED" -> "Aprovado";
            case "SENT_TO_SUPPLIER" -> "Enviado ao Fornecedor";
            case "CANCELLED" -> "Cancelado";
            default -> this.changeType;
        };
    }
}