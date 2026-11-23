package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrderExecutionNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkOrderExecutionNoteRepository extends JpaRepository<WorkOrderExecutionNote, UUID> {
    List<WorkOrderExecutionNote> findByWorkOrderIdOrderByCreatedAtDescIdDesc(UUID workOrderId);
}
