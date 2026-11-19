package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.alfatahi.erp.entity.converter.PaymentBreakdownConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;

@Entity
@Table(name = "quotes")
public class Quote {

    public static final int DEFAULT_DELIVERY_BUSINESS_DAYS = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @ManyToOne
    @JoinColumn(name = "client_id")
    @JsonIgnoreProperties({"quotes", "workOrders"})
    private Client client;

    @Column(nullable = false, length = 50)
    private String status = "pending";

    @Column(name = "total_value", precision = 12, scale = 2)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "seller_name", length = 100)
    private String sellerName;

    @Column(columnDefinition = "text")
    private String warranty;

    @Column(name = "payment_method", length = 255)
    private String paymentMethod;

    private Integer installments = 1;

    @Convert(converter = PaymentBreakdownConverter.class)
    @Column(name = "payment_breakdown", columnDefinition = "text")
    private Map<String, BigDecimal> paymentBreakdown = new LinkedHashMap<>();

    @Convert(converter = PaymentBreakdownConverter.class)
    @Column(name = "payment_composition", columnDefinition = "text")
    private Map<String, BigDecimal> paymentComposition = new LinkedHashMap<>();

    @Column(name = "cash_discount_type", length = 12)
    private String cashDiscountType = "PERCENT";
    @Column(name = "cash_discount_value", precision = 12, scale = 6)
    private BigDecimal cashDiscountValue = BigDecimal.ZERO;
    @Column(name = "pix_discount_type", length = 12)
    private String pixDiscountType = "PERCENT";
    @Column(name = "pix_discount_value", precision = 12, scale = 6)
    private BigDecimal pixDiscountValue = BigDecimal.ZERO;
    @Column(name = "credit_installments")
    private Integer creditInstallments = 0;
    @Column(name = "credit_with_interest")
    private Boolean creditWithInterest = false;
    @Column(name = "credit_monthly_interest_rate", precision = 8, scale = 4)
    private BigDecimal creditMonthlyInterestRate;
    @Column(name = "boleto_enabled")
    private Boolean boletoEnabled = false;
    @Column(name = "boleto_value", precision = 12, scale = 2)
    private BigDecimal boletoValue;
    @Column(name = "boleto_installments")
    private Integer boletoInstallments = 0;
    @Column(name = "boleto_first_due_date")
    private LocalDate boletoFirstDueDate;
    @Column(name = "boleto_interval_days")
    private Integer boletoIntervalDays;
    @Column(name = "approved_sale_total", precision = 12, scale = 2)
    private BigDecimal approvedSaleTotal;
    @Column(name = "agreed_credit_installments")
    private Integer agreedCreditInstallments;
    @Column(name = "agreed_credit_with_interest")
    private Boolean agreedCreditWithInterest;
    @Column(name = "agreed_credit_monthly_interest_rate", precision = 8, scale = 4)
    private BigDecimal agreedCreditMonthlyInterestRate;

    @Column(name = "payment_plan", length = 20)
    private String paymentPlan = "SPLIT_50_50";

    public String getPaymentPlan() { return paymentPlan != null ? paymentPlan : "SPLIT_50_50"; }
    public void setPaymentPlan(String paymentPlan) { this.paymentPlan = paymentPlan; }

    @Column(name = "date_created")
    private LocalDateTime dateCreated = LocalDateTime.now();

    @Column(name = "date_approved")
    private LocalDateTime dateApproved;

    /** Data até a qual o orçamento é válido (inclusive). Aparece no PDF. */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "delivery_business_days")
    private Integer deliveryBusinessDays = DEFAULT_DELIVERY_BUSINESS_DAYS;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("quote")
    private List<QuoteItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "quote")
    @JsonIgnoreProperties("quote")
    private WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Profile profile;

    @Column(name = "public_token", unique = true, updatable = false)
    private String publicToken = UUID.randomUUID().toString();

    @Column(name = "client_signature", columnDefinition = "text")
    private String clientSignature;

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }

    public String getClientSignature() { return clientSignature; }
    public void setClientSignature(String clientSignature) { this.clientSignature = clientSignature; }

    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getWarranty() { return warranty; }
    public void setWarranty(String warranty) { this.warranty = warranty; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }

    public Map<String, BigDecimal> getPaymentBreakdown() { return paymentBreakdown; }
    public void setPaymentBreakdown(Map<String, BigDecimal> paymentBreakdown) {
        this.paymentBreakdown = paymentBreakdown != null ? new LinkedHashMap<>(paymentBreakdown) : new LinkedHashMap<>();
    }

    public Map<String, BigDecimal> getPaymentComposition() { return paymentComposition; }
    public void setPaymentComposition(Map<String, BigDecimal> paymentComposition) {
        this.paymentComposition = paymentComposition != null ? new LinkedHashMap<>(paymentComposition) : new LinkedHashMap<>();
    }
    public String getCashDiscountType() { return cashDiscountType != null ? cashDiscountType : "PERCENT"; }
    public void setCashDiscountType(String cashDiscountType) { this.cashDiscountType = cashDiscountType; }
    public BigDecimal getCashDiscountValue() { return cashDiscountValue != null ? cashDiscountValue : BigDecimal.ZERO; }
    public void setCashDiscountValue(BigDecimal cashDiscountValue) { this.cashDiscountValue = cashDiscountValue; }
    public String getPixDiscountType() { return pixDiscountType != null ? pixDiscountType : "PERCENT"; }
    public void setPixDiscountType(String pixDiscountType) { this.pixDiscountType = pixDiscountType; }
    public BigDecimal getPixDiscountValue() { return pixDiscountValue != null ? pixDiscountValue : BigDecimal.ZERO; }
    public void setPixDiscountValue(BigDecimal pixDiscountValue) { this.pixDiscountValue = pixDiscountValue; }
    public Integer getCreditInstallments() { return creditInstallments != null ? creditInstallments : 0; }
    public void setCreditInstallments(Integer creditInstallments) { this.creditInstallments = creditInstallments; }
    public Boolean getCreditWithInterest() { return Boolean.TRUE.equals(creditWithInterest); }
    public void setCreditWithInterest(Boolean creditWithInterest) { this.creditWithInterest = creditWithInterest; }
    public BigDecimal getCreditMonthlyInterestRate() { return creditMonthlyInterestRate; }
    public void setCreditMonthlyInterestRate(BigDecimal creditMonthlyInterestRate) { this.creditMonthlyInterestRate = creditMonthlyInterestRate; }
    public Boolean getBoletoEnabled() { return Boolean.TRUE.equals(boletoEnabled); }
    public void setBoletoEnabled(Boolean boletoEnabled) { this.boletoEnabled = boletoEnabled; }
    public BigDecimal getBoletoValue() { return boletoValue; }
    public void setBoletoValue(BigDecimal boletoValue) { this.boletoValue = boletoValue; }
    public Integer getBoletoInstallments() { return boletoInstallments != null ? boletoInstallments : 0; }
    public void setBoletoInstallments(Integer boletoInstallments) { this.boletoInstallments = boletoInstallments; }
    public LocalDate getBoletoFirstDueDate() { return boletoFirstDueDate; }
    public void setBoletoFirstDueDate(LocalDate boletoFirstDueDate) { this.boletoFirstDueDate = boletoFirstDueDate; }
    public Integer getBoletoIntervalDays() { return boletoIntervalDays; }
    public void setBoletoIntervalDays(Integer boletoIntervalDays) { this.boletoIntervalDays = boletoIntervalDays; }
    public BigDecimal getApprovedSaleTotal() { return approvedSaleTotal; }
    public void setApprovedSaleTotal(BigDecimal approvedSaleTotal) { this.approvedSaleTotal = approvedSaleTotal; }
    public Integer getAgreedCreditInstallments() { return agreedCreditInstallments; }
    public void setAgreedCreditInstallments(Integer agreedCreditInstallments) { this.agreedCreditInstallments = agreedCreditInstallments; }
    public Boolean getAgreedCreditWithInterest() { return Boolean.TRUE.equals(agreedCreditWithInterest); }
    public void setAgreedCreditWithInterest(Boolean agreedCreditWithInterest) { this.agreedCreditWithInterest = agreedCreditWithInterest; }
    public BigDecimal getAgreedCreditMonthlyInterestRate() { return agreedCreditMonthlyInterestRate; }
    public void setAgreedCreditMonthlyInterestRate(BigDecimal agreedCreditMonthlyInterestRate) { this.agreedCreditMonthlyInterestRate = agreedCreditMonthlyInterestRate; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }

    public Integer getDeliveryBusinessDays() { return deliveryBusinessDays; }
    public void setDeliveryBusinessDays(Integer deliveryBusinessDays) { this.deliveryBusinessDays = deliveryBusinessDays; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

    public LocalDateTime getDateApproved() { return dateApproved; }
    public void setDateApproved(LocalDateTime dateApproved) { this.dateApproved = dateApproved; }

    public List<QuoteItem> getItems() { return items; }
    public void setItems(List<QuoteItem> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
    }
}
