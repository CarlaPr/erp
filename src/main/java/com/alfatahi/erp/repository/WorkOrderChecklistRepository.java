package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrderChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkOrderChecklistRepository extends JpaRepository<WorkOrderChecklist, UUID> {}
