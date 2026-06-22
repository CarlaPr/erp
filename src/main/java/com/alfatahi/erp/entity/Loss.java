package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "losses")
public class Loss {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder; // Obra onde ocorreu o prejuízo

    @Column(nullable = false)
    private String material; // Ex: Espelho 4mm, Perfil U

    @Column(nullable = false)
    private String reason; // Quebra no transporte, erro de medida, defeito de fábrica

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal financialImpact = BigDecimal.ZERO;

    private LocalDate occurrenceDate = LocalDate.now();

    // Getters e Setters gerados
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public WorkOrder getWorkOrder() { return workOrder; } public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public String getMaterial() { return material; } public void setMaterial(String material) { this.material = material; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getFinancialImpact() { return financialImpact; } public void setFinancialImpact(BigDecimal financialImpact) { this.financialImpact = financialImpact; }
    public LocalDate getOccurrenceDate() { return occurrenceDate; } public void setOccurrenceDate(LocalDate occurrenceDate) { this.occurrenceDate = occurrenceDate; }
}