package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlanHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanHistoryRepository extends JpaRepository<CutPlanHistory, UUID> {
    List<CutPlanHistory> findByCutPlanIdOrderByChangedAtDesc(UUID cutPlanId);
}
