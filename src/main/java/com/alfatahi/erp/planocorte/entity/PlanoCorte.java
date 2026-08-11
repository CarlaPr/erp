package com.alfatahi.erp.planocorte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.alfatahi.erp.entity.WorkOrder;

@Entity
@Table(name = "planos_corte")
public class PlanoCorte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaServico categoria;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPlano status;

    @Column(name = "area_total_m2", nullable = false, precision = 12, scale = 4)
    private BigDecimal areaTotalM2 = BigDecimal.ZERO;

    @Column(name = "peso_total_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal pesoTotalKg = BigDecimal.ZERO;

    @Column(name = "valor_total_plano", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotalPlano = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PlanoCorte() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Transient
    public String getNumeroFormatado() {
        return String.format("PC-%05d", id != null ? id : 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public CategoriaServico getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaServico categoria) {
        this.categoria = categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public StatusPlano getStatus() {
        return status;
    }

    public void setStatus(StatusPlano status) {
        this.status = status;
    }

    public BigDecimal getAreaTotalM2() {
        return areaTotalM2;
    }

    public void setAreaTotalM2(BigDecimal areaTotalM2) {
        this.areaTotalM2 = areaTotalM2;
    }

    public BigDecimal getPesoTotalKg() {
        return pesoTotalKg;
    }

    public void setPesoTotalKg(BigDecimal pesoTotalKg) {
        this.pesoTotalKg = pesoTotalKg;
    }

    public BigDecimal getValorTotalPlano() {
        return valorTotalPlano;
    }

    public void setValorTotalPlano(BigDecimal valorTotalPlano) {
        this.valorTotalPlano = valorTotalPlano;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
