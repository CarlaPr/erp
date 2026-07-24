package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CutPlanApprovalResponse - Confirmação de aprovação
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanApprovalResponse {

    private UUID cutPlanId;
    private String newStatus;
    private Integer newVersion;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private Boolean notificationsDelivered;
}
