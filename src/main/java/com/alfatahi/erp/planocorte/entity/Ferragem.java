package com.alfatahi.erp.planocorte.entity;

import com.alfatahi.erp.entity.Supplier;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ferragens")
public class Ferragem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(nullable = false, length = 150)
    private String produto;

    @Column(length = 50)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(length = 20)
    private String unidade = "un";



    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_elemento_padrao", length = 20)
    private TipoElemento tipoElementoPadrao;

    @Column(name = "diametro_furacao_mm", precision = 8, scale = 2)
    private BigDecimal diametroFuracaoMm;

    @Column(name = "quantidade_furos")
    private Integer quantidadeFuros;

    @Column(name = "distancia_borda_mm", precision = 8, scale = 2)
    private BigDecimal distanciaBordaMm;

    @Column(name = "distancia_topo_mm", precision = 8, scale = 2)
    private BigDecimal distanciaTopoMm;




    @Column(name = "largura_mm", precision = 8, scale = 2)
    private BigDecimal larguraMm;

    @Column(name = "altura_mm", precision = 8, scale = 2)
    private BigDecimal alturaMm;

    @Column(name = "comprimento_mm", precision = 8, scale = 2)
    private BigDecimal comprimentoMm;

    @Column(name = "profundidade_mm", precision = 8, scale = 2)
    private BigDecimal profundidadeMm;

    @Column(name = "raio_mm", precision = 8, scale = 2)
    private BigDecimal raioMm;

    @Column(name = "angulo_graus", precision = 6, scale = 2)
    private BigDecimal anguloGraus;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Ferragem() {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getProduto() {
        return produto;
    }

    public void setProduto(String produto) {
        this.produto = produto;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public void setUnidade(String unidade) {
        this.unidade = unidade;
    }

    public TipoElemento getTipoElementoPadrao() {
        return tipoElementoPadrao;
    }

    public void setTipoElementoPadrao(TipoElemento tipoElementoPadrao) {
        this.tipoElementoPadrao = tipoElementoPadrao;
    }

    public BigDecimal getDiametroFuracaoMm() {
        return diametroFuracaoMm;
    }

    public void setDiametroFuracaoMm(BigDecimal diametroFuracaoMm) {
        this.diametroFuracaoMm = diametroFuracaoMm;
    }

    public Integer getQuantidadeFuros() {
        return quantidadeFuros;
    }

    public void setQuantidadeFuros(Integer quantidadeFuros) {
        this.quantidadeFuros = quantidadeFuros;
    }

    public BigDecimal getDistanciaBordaMm() {
        return distanciaBordaMm;
    }

    public void setDistanciaBordaMm(BigDecimal distanciaBordaMm) {
        this.distanciaBordaMm = distanciaBordaMm;
    }

    public BigDecimal getDistanciaTopoMm() {
        return distanciaTopoMm;
    }

    public void setDistanciaTopoMm(BigDecimal distanciaTopoMm) {
        this.distanciaTopoMm = distanciaTopoMm;
    }

    public BigDecimal getLarguraMm() {
        return larguraMm;
    }

    public void setLarguraMm(BigDecimal larguraMm) {
        this.larguraMm = larguraMm;
    }

    public BigDecimal getAlturaMm() {
        return alturaMm;
    }

    public void setAlturaMm(BigDecimal alturaMm) {
        this.alturaMm = alturaMm;
    }

    public BigDecimal getComprimentoMm() {
        return comprimentoMm;
    }

    public void setComprimentoMm(BigDecimal comprimentoMm) {
        this.comprimentoMm = comprimentoMm;
    }

    public BigDecimal getProfundidadeMm() {
        return profundidadeMm;
    }

    public void setProfundidadeMm(BigDecimal profundidadeMm) {
        this.profundidadeMm = profundidadeMm;
    }

    public BigDecimal getRaioMm() {
        return raioMm;
    }

    public void setRaioMm(BigDecimal raioMm) {
        this.raioMm = raioMm;
    }

    public BigDecimal getAnguloGraus() {
        return anguloGraus;
    }

    public void setAnguloGraus(BigDecimal anguloGraus) {
        this.anguloGraus = anguloGraus;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
