package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlanItemDrilling;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanItemDrillingRepository extends JpaRepository<CutPlanItemDrilling, UUID> {
    List<CutPlanItemDrilling> findByCutPlanItemId(UUID cutPlanItemId);
}
