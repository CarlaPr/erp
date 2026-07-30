package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Subcategoria vinculada a uma categoria principal de despesa.
 * Suporta um agrupamento opcional ({@code groupLabel}) e um segmento opcional
 * ({@code segment}), usados para as regras específicas de vidraçaria
 * (ex.: Insumos, Ferragens, Alumínio, Serviços).
 */
@Entity
@Table(name = "expense_subcategories")
public class ExpenseSubcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnoreProperties({"subcategories"})
    private ExpenseCategory category;

    /** Código estável usado como valor persistido em accounts_payable.subcategory. */
    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    /** Agrupamento visual opcional (ex.: "Insumos", "Ferragens", "Alumínio", "Serviços"). */
    @Column(name = "group_label", length = 60)
    private String groupLabel;

    /** Segmento de negócio opcional (ex.: "VIDRACARIA") para regras específicas de nicho. */
    @Column(name = "segment", length = 40)
    private String segment;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean active = true;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupLabel() { return groupLabel; }
    public void setGroupLabel(String groupLabel) { this.groupLabel = groupLabel; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getActive() { return active != null ? active : true; }
    public void setActive(Boolean active) { this.active = active; }
}
