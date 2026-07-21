package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conjunto de regras de desconto/folga técnica, totalmente parametrizável
 * pelo administrador (nenhum valor de desconto fica fixo no código).
 * Pode ser associado a uma categoria de serviço (Box, Janela, Espelho...)
 * ou ficar genérico (serviceCategory = null).
 */
@Entity
@Table(name = "cut_rule_sets")
public class CutRuleSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne
    @JoinColumn(name = "service_category_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ServiceCategory serviceCategory;

    @Column(name = "glass_type", length = 100)
    private String glassType;

    private String description;

    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "ruleSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @JsonIgnoreProperties("ruleSet")
    private List<CutRuleParameter> parameters = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ServiceCategory getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(ServiceCategory serviceCategory) { this.serviceCategory = serviceCategory; }
    public String getGlassType() { return glassType; }
    public void setGlassType(String glassType) { this.glassType = glassType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<CutRuleParameter> getParameters() { return parameters; }
    public void setParameters(List<CutRuleParameter> parameters) {
        this.parameters.clear();
        if (parameters != null) this.parameters.addAll(parameters);
    }
}
