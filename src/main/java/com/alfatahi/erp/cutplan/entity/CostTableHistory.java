package com.alfatahi.erp.cutplan.entity;

import com.alfatahi.erp.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * COSTTABLEHISTORY - Entidade para auditoria de mudanças de preço
 * Rastreia histórico de alterações de preços para fins de auditoria
 * e análise de variação de custos
 *
 * Relacionamentos:
 * - N:1 com CostTable
 * - N:1 com AppUser (changedBy)
 */
@Entity
@Table(
        name = "cost_table_history",
        indexes = {
                @Index(name = "idx_cost_table_history_cost_table_id", columnList = "cost_table_id"),
                @Index(name = "idx_cost_table_history_changed_by", columnList = "changed_by"),
                @Index(name = "idx_cost_table_history_changed_at", columnList = "changed_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CostTableHistory {

    @Id
    @UuidGenerator
    private UUID id;

    /**
     * Referência para a Tabela de Preço
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cost_table_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cost_table_history_cost_table_id")
    )
    private CostTable costTable;

    /**
     * Preço anterior (antes da mudança)
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal oldPrice;

    /**
     * Preço novo (depois da mudança)
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newPrice;

    /**
     * Usuário que fez a mudança
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "changed_by",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_cost_table_history_changed_by")
    )
    private AppUser changedBy;

    /**
     * Data/hora da mudança
     */
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /**
     * Motivo da mudança
     * Exemplos: "Ajuste mensal", "Nova tabela do fornecedor", "Promoção", etc
     */
    @Column(columnDefinition = "TEXT")
    private String reason;

    /**
     * Referência de documento ou observação adicional
     */
    @Column(length = 500)
    private String reference;

    /**
     * ========== MÉTODOS ==========
     */

    /**
     * Calcula a diferença absoluta entre preços
     */
    public BigDecimal getAbsoluteDifference() {
        return this.newPrice.subtract(this.oldPrice);
    }

    /**
     * Calcula a variação percentual
     * Resultado positivo = aumento, negativo = redução
     */
    public BigDecimal getPercentageDifference() {
        if (this.oldPrice.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }

        return this.newPrice
                .subtract(this.oldPrice)
                .divide(this.oldPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * Indica se houve aumento de preço
     */
    public boolean isIncrease() {
        return this.newPrice.compareTo(this.oldPrice) > 0;
    }

    /**
     * Indica se houve redução de preço
     */
    public boolean isDecrease() {
        return this.newPrice.compareTo(this.oldPrice) < 0;
    }

    /**
     * Indica se houve mudança de preço
     */
    public boolean hasChanged() {
        return this.newPrice.compareTo(this.oldPrice) != 0;
    }

    /**
     * Retorna descrição formatada da mudança
     * Exemplo: "R$ 150,00 → R$ 165,00 (+10,00%)"
     */
    public String getFormattedChange() {
        BigDecimal pct = getPercentageDifference();
        String sign = isIncrease() ? "+" : "";

        return String.format(
                "R$ %,.2f → R$ %,.2f (%s%,.2f%%)",
                this.oldPrice,
                this.newPrice,
                sign,
                pct
        );
    }

    /**
     * Retorna descrição em português do tipo de mudança
     */
    public String getChangeDescription() {
        if (isIncrease()) {
            return "Aumento de " + getPercentageDifference() + "%";
        } else if (isDecrease()) {
            return "Redução de " + getPercentageDifference().abs() + "%";
        } else {
            return "Sem alteração";
        }
    }
}