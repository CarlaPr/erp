package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkOrderItemRepository extends JpaRepository<WorkOrderItem, UUID> {
    List<WorkOrderItem> findByWorkOrderId(UUID workOrderId);
}