package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
}