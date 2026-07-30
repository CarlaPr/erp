package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ExpenseRecurrence;
import com.alfatahi.erp.entity.RecurrenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRecurrenceRepository extends JpaRepository<ExpenseRecurrence, UUID> {
    List<ExpenseRecurrence> findByStatus(RecurrenceStatus status);
}
