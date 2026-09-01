package com.alfatahi.erp.dto;

/**
 * Resumo do status de pagamento (contas a receber) de uma Ordem de Serviço específica,
 * usado para exibir um selo como "OS-1231 - Pago entrada" ou "OS-1234 - Pagamento pendente"
 * no card de cada O.S. Cada O.S. tem seu próprio resumo, calculado a partir apenas das
 * contas a receber vinculadas a ela.
 */
public class WorkOrderPaymentStatusDto {

    private String label;
    private String badgeClass;

    public WorkOrderPaymentStatusDto() {
    }

    public WorkOrderPaymentStatusDto(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getBadgeClass() { return badgeClass; }
    public void setBadgeClass(String badgeClass) { this.badgeClass = badgeClass; }
}
