package com.alfatahi.erp.planocorte.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "historico_preco_vidro")
public class HistoricoPrecoVidro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vidro_id", nullable = false)
    private Long vidroId;

    @Column(name = "vidro_nome_snapshot", nullable = false, length = 150)
    private String vidroNomeSnapshot;

    @Column(name = "valor_antigo", precision = 12, scale = 2)
    private BigDecimal valorAntigo;

    @Column(name = "valor_novo", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorNovo;

    @Column(nullable = false, length = 60)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemHistorico origem;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public HistoricoPrecoVidro() {
    }

    @PrePersist
    protected void onCreate() {
        this.criadoEm = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getValorAntigo() {
        return valorAntigo;
    }

    public void setValorAntigo(BigDecimal valorAntigo) {
        this.valorAntigo = valorAntigo;
    }

    public BigDecimal getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(BigDecimal valorNovo) {
        this.valorNovo = valorNovo;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public OrigemHistorico getOrigem() {
        return origem;
    }

    public void setOrigem(OrigemHistorico origem) {
        this.origem = origem;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
