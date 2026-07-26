package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * CutPlanApprovalRequest - Dados para aprovação de plano
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanApprovalRequest {

    private UUID cutPlanId;
    private String approvalReason;
    private Boolean includeSupplierNotification;
    private List<String> supplierEmails;
}
