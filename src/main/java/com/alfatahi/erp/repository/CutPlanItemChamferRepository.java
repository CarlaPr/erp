package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlanItemChamfer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanItemChamferRepository extends JpaRepository<CutPlanItemChamfer, UUID> {
    List<CutPlanItemChamfer> findByCutPlanItemId(UUID cutPlanItemId);
}
