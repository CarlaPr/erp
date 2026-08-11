package com.alfatahi.erp.planocorte.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "plano_corte_itens")
public class PlanoCorteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_corte_id", nullable = false)
    private PlanoCorte planoCorte;

    @Column(name = "vidro_id")
    private Long vidroId;

    @Column(name = "vidro_nome_snapshot", nullable = false, length = 150)
    private String vidroNomeSnapshot;

    @Column(name = "espessura_snapshot", precision = 6, scale = 2)
    private BigDecimal espessuraSnapshot;

    @Column(name = "cor_snapshot", length = 50)
    private String corSnapshot;

    @Column(name = "valor_m2_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorM2Snapshot;

    @Column(name = "valor_minimo_snapshot", precision = 12, scale = 2)
    private BigDecimal valorMinimoSnapshot;

    @Column(name = "peso_m2_snapshot", precision = 8, scale = 3)
    private BigDecimal pesoM2Snapshot;

    @Column(name = "largura_bruta_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal larguraBrutaMm;

    @Column(name = "altura_bruta_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal alturaBrutaMm;

    @Column(nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_borda", nullable = false, length = 20)
    private TipoBorda tipoBorda;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "largura_final_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal larguraFinalMm;

    @Column(name = "altura_final_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal alturaFinalMm;

    @Column(name = "area_m2", nullable = false, precision = 12, scale = 4)
    private BigDecimal areaM2;



    @Column(name = "espessura_bisote_mm", precision = 8, scale = 2)
    private BigDecimal espessuraBisoteMm;

    @Column(name = "peso_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal;


    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_folha", nullable = false, length = 10)
    private TipoFolha tipoFolha = TipoFolha.UNICA;


    @Column(name = "grupo_vao")
    private Integer grupoVao;


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plano_corte_item_furacoes", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "ordem")
    private List<Furacao> furacoes = new ArrayList<>();




    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plano_corte_item_elementos", joinColumns = @JoinColumn(name = "item_id"))
    @OrderColumn(name = "ordem")
    private List<ElementoTecnico> elementos = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PlanoCorteItem() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Transient
    public String getCodigoPeca() {
        return String.format("VID-%04d", id != null ? id : 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PlanoCorte getPlanoCorte() {
        return planoCorte;
    }

    public void setPlanoCorte(PlanoCorte planoCorte) {
        this.planoCorte = planoCorte;
    }

    public Long getVidroId() {
        return vidroId;
    }

    public void setVidroId(Long vidroId) {
        this.vidroId = vidroId;
    }

    public String getVidroNomeSnapshot() {
        return vidroNomeSnapshot;
    }

    public void setVidroNomeSnapshot(String vidroNomeSnapshot) {
        this.vidroNomeSnapshot = vidroNomeSnapshot;
    }

    public BigDecimal getEspessuraSnapshot() {
        return espessuraSnapshot;
    }

    public void setEspessuraSnapshot(BigDecimal espessuraSnapshot) {
        this.espessuraSnapshot = espessuraSnapshot;
    }

    public String getCorSnapshot() {
        return corSnapshot;
    }

    public void setCorSnapshot(String corSnapshot) {
        this.corSnapshot = corSnapshot;
    }

    public BigDecimal getValorM2Snapshot() {
        return valorM2Snapshot;
    }

    public void setValorM2Snapshot(BigDecimal valorM2Snapshot) {
        this.valorM2Snapshot = valorM2Snapshot;
    }

    public BigDecimal getValorMinimoSnapshot() {
        return valorMinimoSnapshot;
    }

    public void setValorMinimoSnapshot(BigDecimal valorMinimoSnapshot) {
        this.valorMinimoSnapshot = valorMinimoSnapshot;
    }

    public BigDecimal getPesoM2Snapshot() {
        return pesoM2Snapshot;
    }

    public void setPesoM2Snapshot(BigDecimal pesoM2Snapshot) {
        this.pesoM2Snapshot = pesoM2Snapshot;
    }

    public BigDecimal getLarguraBrutaMm() {
        return larguraBrutaMm;
    }

    public void setLarguraBrutaMm(BigDecimal larguraBrutaMm) {
        this.larguraBrutaMm = larguraBrutaMm;
    }

    public BigDecimal getAlturaBrutaMm() {
        return alturaBrutaMm;
    }

    public void setAlturaBrutaMm(BigDecimal alturaBrutaMm) {
        this.alturaBrutaMm = alturaBrutaMm;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public TipoBorda getTipoBorda() {
        return tipoBorda;
    }

    public void setTipoBorda(TipoBorda tipoBorda) {
        this.tipoBorda = tipoBorda;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public BigDecimal getLarguraFinalMm() {
        return larguraFinalMm;
    }

    public void setLarguraFinalMm(BigDecimal larguraFinalMm) {
        this.larguraFinalMm = larguraFinalMm;
    }

    public BigDecimal getAlturaFinalMm() {
        return alturaFinalMm;
    }

    public void setAlturaFinalMm(BigDecimal alturaFinalMm) {
        this.alturaFinalMm = alturaFinalMm;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public void setAreaM2(BigDecimal areaM2) {
        this.areaM2 = areaM2;
    }

    public BigDecimal getEspessuraBisoteMm() {
        return espessuraBisoteMm;
    }

    public void setEspessuraBisoteMm(BigDecimal espessuraBisoteMm) {
        this.espessuraBisoteMm = espessuraBisoteMm;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(BigDecimal pesoKg) {
        this.pesoKg = pesoKg;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public TipoFolha getTipoFolha() {
        return tipoFolha;
    }

    public void setTipoFolha(TipoFolha tipoFolha) {
        this.tipoFolha = tipoFolha;
    }

    public Integer getGrupoVao() {
        return grupoVao;
    }

    public void setGrupoVao(Integer grupoVao) {
        this.grupoVao = grupoVao;
    }

    public List<Furacao> getFuracoes() {
        return furacoes;
    }

    public void setFuracoes(List<Furacao> furacoes) {
        this.furacoes = furacoes;
    }

    public List<ElementoTecnico> getElementos() {
        return elementos;
    }

    public void setElementos(List<ElementoTecnico> elementos) {
        this.elementos = elementos;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
