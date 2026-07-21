package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutPlanRepository extends JpaRepository<CutPlan, UUID> {
    List<CutPlan> findByWorkOrderIdOrderByPlanNumberAsc(UUID workOrderId);
    long countByWorkOrderId(UUID workOrderId);
    List<CutPlan> findAllByOrderByCreatedAtDesc();
}
