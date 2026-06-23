package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "losses")
public class Loss {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @Column(nullable = false)
    private String type;

    // ESTE É O CAMPO QUE RESOLVE O ERRO DE SQL!
    @Column(nullable = false)
    private String material = "N/A"; // Valor default seguro

    @Column(nullable = false)
    private String description;

    @Column(name = "financial_impact", nullable = false, precision = 12, scale = 2)
    private BigDecimal financialImpact = BigDecimal.ZERO;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate = LocalDate.now();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getFinancialImpact() { return financialImpact; }
    public void setFinancialImpact(BigDecimal financialImpact) { this.financialImpact = financialImpact; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; }
    public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }
}