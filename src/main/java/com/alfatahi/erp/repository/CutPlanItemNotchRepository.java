package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlanItemNotch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanItemNotchRepository extends JpaRepository<CutPlanItemNotch, UUID> {
    List<CutPlanItemNotch> findByCutPlanItemId(UUID cutPlanItemId);
}
