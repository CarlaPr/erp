package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
public class WorkOrder {
    public static final String DEFAULT_CONDITIONS_OBSERVATIONS = """
            • Materiais produzidos ou adquiridos sob medida para o cliente poderão ter seus respectivos custos cobrados em caso de cancelamento, independentemente da forma de pagamento escolhida.
            • O cliente declara estar de acordo com as medidas, especificações, materiais, valores e demais condições.
            • A execução está limitada aos serviços expressamente descritos nesta Ordem de Serviço. Serviços adicionais ou não previstos deverão ser previamente acordados entre as partes e poderão gerar custos extras.
            • Produtos sob medida não possuem troca ou devolução, conforme o Código de Defesa do Consumidor.
            • Alterações solicitadas pelo cliente após a confirmação do pedido poderão gerar custos adicionais e alteração do prazo de entrega.
            """.strip();

    public static final String DEFAULT_WARRANTY_TERMS = """
            Garantia de 90 dias, a partir da conclusão da instalação, cobrindo apenas regulagens, vedação e defeitos decorrentes da instalação realizada pela
            empresa. Não cobre:
            • Mau uso, acidentes ou impactos;
            • Quebra de vidros ou uso inadequado;
            • Desgaste natural;
            • Falta de manutenção ou limpeza inadequada;
            • Reparos realizados por terceiros;
            • Danos causados por fatores externos ou fenômenos naturais.
            A garantia será cancelada caso o produto seja alterado ou reparado por pessoas não autorizadas.
            """.strip();

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String number;
    private String title;
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "installation_address", columnDefinition = "TEXT")
    private String installationAddress;

    @Column(name = "site_contact", length = 150)
    private String siteContact;

    @Column(name = "site_phone", length = 50)
    private String sitePhone;

    @Column(name = "access_instructions", columnDefinition = "TEXT")
    private String accessInstructions;

    @Column(name = "technician_name", length = 150)
    private String technicianName;

    @Column(name = "installation_notes", columnDefinition = "TEXT")
    private String installationNotes;

    @Column(name = "conditions_observations", columnDefinition = "TEXT")
    private String conditionsObservations = DEFAULT_CONDITIONS_OBSERVATIONS;

    @Column(name = "warranty_terms", columnDefinition = "TEXT")
    private String warrantyTerms = DEFAULT_WARRANTY_TERMS;

    @Column(name = "show_measurements", nullable = false)
    private boolean showMeasurements = true;
    private BigDecimal totalValue = BigDecimal.ZERO;
    @Column(name = "deadline_date")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate deadlineDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDate installDate;

    @ManyToOne
    @JsonIgnoreProperties({
            "quotes",
            "workOrders"
    })
    private Client client;

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnoreProperties("workOrder")
    private List<WorkOrderItem> items = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "quote_id")
    @JsonIgnore
    private Quote quote;

    @Transient
    @JsonProperty("quoteId")
    private UUID quoteId;

    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal area;

    @ManyToOne
    private ServiceCategory category;

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal width) { this.width = width; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getInstallationAddress() { return installationAddress; }
    public void setInstallationAddress(String installationAddress) { this.installationAddress = installationAddress; }
    public String getSiteContact() { return siteContact; }
    public void setSiteContact(String siteContact) { this.siteContact = siteContact; }
    public String getSitePhone() { return sitePhone; }
    public void setSitePhone(String sitePhone) { this.sitePhone = sitePhone; }
    public String getAccessInstructions() { return accessInstructions; }
    public void setAccessInstructions(String accessInstructions) { this.accessInstructions = accessInstructions; }
    public String getTechnicianName() { return technicianName; }
    public void setTechnicianName(String technicianName) { this.technicianName = technicianName; }
    public String getInstallationNotes() { return installationNotes; }
    public void setInstallationNotes(String installationNotes) { this.installationNotes = installationNotes; }
    public String getConditionsObservations() { return conditionsObservations; }
    public void setConditionsObservations(String conditionsObservations) { this.conditionsObservations = conditionsObservations; }
    public String getWarrantyTerms() { return warrantyTerms; }
    public void setWarrantyTerms(String warrantyTerms) { this.warrantyTerms = warrantyTerms; }
    public boolean isShowMeasurements() { return showMeasurements; }
    public void setShowMeasurements(boolean showMeasurements) { this.showMeasurements = showMeasurements; }
    public BigDecimal getTotalValue() { return totalValue != null ? totalValue : BigDecimal.ZERO; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }
    public LocalDate getInstallDate() { return installDate; }
    public void setInstallDate(LocalDate installDate) { this.installDate = installDate; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public List<WorkOrderItem> getItems() { return items; }
    public void setItems(List<WorkOrderItem> items) { this.items = items; }
    public Quote getQuote() { return quote; }
    public void setQuote(Quote quote) { this.quote = quote; }
    public UUID getQuoteId() { return (this.quoteId != null) ? this.quoteId : (this.quote != null ? this.quote.getId() : null); }
    public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }

    public BigDecimal getTotalCost() { return items.stream().map(WorkOrderItem::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal getProfit() { return getTotalValue().subtract(getTotalCost()); }
    public String getMargin() { return getTotalValue().compareTo(BigDecimal.ZERO) > 0 ? getProfit().multiply(new BigDecimal("100")).divide(getTotalValue(), 1, java.math.RoundingMode.HALF_UP).toString() : "0"; }

}
