package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanItemRepository extends JpaRepository<CutPlanItem, UUID> {
    List<CutPlanItem> findByCutPlanIdOrderBySortOrderAsc(UUID cutPlanId);
}
