package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CutPlanBatchOperationResult {

    private String operationType;
    private Integer totalProcessed;
    private Integer successful;
    private Integer failed;
    private List<String> errors;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public Integer getDuration() {
        if (completedAt == null || startedAt == null) return 0;
        return (int) java.time.Duration.between(startedAt, completedAt).getSeconds();
    }

    public Boolean isSuccess() {
        return failed == 0 || failed == null;
    }
}
