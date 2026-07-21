package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Histórico imutável de alterações de preço de um insumo do catálogo. */
@Entity
@Table(name = "material_price_history")
public class MaterialPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "price_item_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MaterialPriceItem priceItem;

    @Column(name = "old_price", precision = 12, scale = 4)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 12, scale = 4)
    private BigDecimal newPrice;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt = LocalDateTime.now();

    private String reason;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public MaterialPriceItem getPriceItem() { return priceItem; }
    public void setPriceItem(MaterialPriceItem priceItem) { this.priceItem = priceItem; }
    public BigDecimal getOldPrice() { return oldPrice; }
    public void setOldPrice(BigDecimal oldPrice) { this.oldPrice = oldPrice; }
    public BigDecimal getNewPrice() { return newPrice; }
    public void setNewPrice(BigDecimal newPrice) { this.newPrice = newPrice; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
