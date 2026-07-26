package com.alfatahi.erp.cutplan.service;

import java.time.LocalDateTime;

/**
 * DTO: Estatísticas de histórico
 */
@lombok.Data
@lombok.Builder
class CutPlanHistoryStatistics {
    private int totalChanges;
    private int createdCount;
    private int itemAddedCount;
    private int itemUpdatedCount;
    private int itemRemovedCount;
    private int costRecalcCount;
    private int statusChangedCount;
    private int approvedCount;
    private int sentToSupplierCount;
    private String firstUserToChange;
    private String lastUserToChange;
    private LocalDateTime oldestChangeAt;
    private LocalDateTime latestChangeAt;
}
