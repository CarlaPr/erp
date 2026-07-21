package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Um parâmetro individual de desconto/folga (ex: "Desconto lateral").
 * dimension define se o valor é subtraído da LARGURA, ALTURA ou de AMBAS
 * ao calcular a medida final de corte a partir da medida bruta (vão).
 */
@Entity
@Table(name = "cut_rule_parameters")
public class CutRuleParameter {

    public enum Dimension { WIDTH, HEIGHT, BOTH }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "rule_set_id", nullable = false)
    private CutRuleSet ruleSet;

    @Column(name = "param_key", nullable = false, length = 100)
    private String paramKey;

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dimension dimension = Dimension.BOTH;

    @Column(name = "value_mm", nullable = false, precision = 10, scale = 2)
    private BigDecimal valueMm = BigDecimal.ZERO;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public CutRuleSet getRuleSet() { return ruleSet; }
    public void setRuleSet(CutRuleSet ruleSet) { this.ruleSet = ruleSet; }
    public String getParamKey() { return paramKey; }
    public void setParamKey(String paramKey) { this.paramKey = paramKey; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Dimension getDimension() { return dimension; }
    public void setDimension(Dimension dimension) { this.dimension = dimension; }
    public BigDecimal getValueMm() { return valueMm != null ? valueMm : BigDecimal.ZERO; }
    public void setValueMm(BigDecimal valueMm) { this.valueMm = valueMm; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
