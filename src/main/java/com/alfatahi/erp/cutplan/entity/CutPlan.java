package com.alfatahi.erp.cutplan.entity;


import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * CUTPLAN - Entidade principal para Plano de Corte
 * Representa o plano de corte gerado para uma Ordem de Serviço
 *
 * Relacionamentos:
 * - 1:1 com WorkOrder
 * - 1:N com CutPlanItem
 * - 1:N com CutPlanHistory
 * - N:1 com AppUser (createdBy, updatedBy)
 */
@Entity
@Table(
        name = "cut_plans",
        indexes = {
                @Index(name = "idx_cut_plans_work_order_id", columnList = "work_order_id"),
                @Index(name = "idx_cut_plans_status", columnList = "status"),
                @Index(name = "idx_cut_plans_created_at", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlan {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Referência para a Ordem de Serviço
     * UNIQUE: um plano de corte por OS (1:1)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "work_order_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_cut_plans_work_order_id")
    )
    private WorkOrder workOrder;

    /**
     * Versão do plano (incrementado a cada alteração major)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    /**
     * Status do plano: DRAFT, APPROVED, SENT_TO_SUPPLIER, CANCELLED
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "DRAFT";

    /**
     * Descrição/observações adicionais
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Itens que compõem o plano de corte
     */
    @OneToMany(
            mappedBy = "cutPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<CutPlanItem> items = new ArrayList<>();

    /**
     * Histórico de alterações
     */
    @OneToMany(
            mappedBy = "cutPlan",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<CutPlanHistory> history = new ArrayList<>();

    /**
     * Usuário que criou o plano
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by",
            foreignKey = @ForeignKey(name = "fk_cut_plans_created_by")
    )
    private AppUser createdBy;

    /**
     * Data/hora de criação
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Usuário que atualizou por último
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "updated_by",
            foreignKey = @ForeignKey(name = "fk_cut_plans_updated_by")
    )
    private AppUser updatedBy;

    /**
     * Data/hora da última atualização
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Métodos de negócio
     */
    public void addItem(CutPlanItem item) {
        this.items.add(item);
        item.setCutPlan(this);
    }

    public void removeItem(CutPlanItem item) {
        this.items.remove(item);
        item.setCutPlan(null);
    }

    public void addHistory(CutPlanHistory historyEntry) {
        this.history.add(historyEntry);
        historyEntry.setCutPlan(this);
    }

    public boolean isDraft() {
        return "DRAFT".equals(this.status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(this.status);
    }

    public boolean isSentToSupplier() {
        return "SENT_TO_SUPPLIER".equals(this.status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(this.status);
    }
}