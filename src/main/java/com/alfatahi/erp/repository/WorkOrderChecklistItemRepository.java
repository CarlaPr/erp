package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrderChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderChecklistItemRepository extends JpaRepository<WorkOrderChecklistItem, UUID> {
    List<WorkOrderChecklistItem> findByWorkOrderIdOrderByCompletedAscCreatedAtAscIdAsc(UUID workOrderId);
    Optional<WorkOrderChecklistItem> findByWorkOrderIdAndId(UUID workOrderId, UUID id);
}
