package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CutPlanHistoryResponse - Resposta com histórico de alterações
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanHistoryResponse {

    private UUID id;
    private String changeType;
    private String changeTypeLabel;
    private String description;
    private Integer version;
    private LocalDateTime changedAt;
    private String changedBy;
    private String oldValues;
    private String newValues;
    private UUID affectedItemId;
    private String affectedItemDescription;
}
