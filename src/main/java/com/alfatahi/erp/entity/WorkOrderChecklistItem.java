package com.alfatahi.erp.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_order_checklist_items")
public class WorkOrderChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "work_order_id", nullable = false, updatable = false)
    private UUID workOrderId;
    @Column(nullable = false, length = 180)
    private String name;
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;
    @Column(columnDefinition = "TEXT")
    private String observation;
    @Column(nullable = false)
    private boolean completed;
    @Column(length = 64, updatable = false)
    private String suggestionKey;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Version
    private long version;

    protected WorkOrderChecklistItem() {}

    public WorkOrderChecklistItem(UUID workOrderId, String name, BigDecimal quantity, String observation,
                                  String suggestionKey) {
        this.workOrderId = workOrderId;
        this.suggestionKey = suggestionKey;
        update(name, quantity, observation);
    }

    public void update(String name, BigDecimal quantity, String observation) {
        this.name = name.strip();
        this.quantity = quantity;
        this.observation = observation == null || observation.isBlank() ? null : observation.strip();
    }

    public void setCompleted(boolean completed) { this.completed = completed; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getQuantity() { return quantity; }
    public String getObservation() { return observation; }
    public boolean isCompleted() { return completed; }
    public String getSuggestionKey() { return suggestionKey; }
    public long getVersion() { return version; }
}
