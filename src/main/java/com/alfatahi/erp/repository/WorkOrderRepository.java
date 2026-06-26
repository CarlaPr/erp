package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    List<WorkOrder> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Transactional
    @Query("""
SELECT COALESCE(
MAX(CAST(SUBSTRING(w.number, 4) AS integer)),
1000
)
FROM WorkOrder w
WHERE w.number LIKE 'OS-%'
""")
    Integer findMaxWorkOrderSequence();
}