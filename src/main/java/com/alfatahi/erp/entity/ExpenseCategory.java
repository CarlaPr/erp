package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Categoria principal de despesa do módulo de Contas a Pagar.
 * Ex.: FIXA, VARIAVEL, PROVISIONAMENTO, IMPOSTOS, INVESTIMENTOS,
 * MANUTENCAO, FINANCEIRO, PESSOAL, OPERACIONAL, ADMINISTRATIVO.
 */
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Código estável usado como valor persistido em accounts_payable.category (ex.: "FIXA"). */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /** Nome de exibição (ex.: "Fixa"). */
    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active")
    private Boolean active = true;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ExpenseSubcategory> subcategories = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public Boolean getActive() { return active != null ? active : true; }
    public void setActive(Boolean active) { this.active = active; }
    public List<ExpenseSubcategory> getSubcategories() { return subcategories; }
    public void setSubcategories(List<ExpenseSubcategory> subcategories) { this.subcategories = subcategories; }
}
