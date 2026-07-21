package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlanMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanMaterialRepository extends JpaRepository<CutPlanMaterial, UUID> {
    List<CutPlanMaterial> findByCutPlanId(UUID cutPlanId);
}
