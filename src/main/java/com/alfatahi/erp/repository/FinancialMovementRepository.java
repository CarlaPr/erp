package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.FinancialMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FinancialMovementRepository extends JpaRepository<FinancialMovement, UUID> {

    List<FinancialMovement> findAllByMovementDateBetweenOrderByMovementDateAscCreatedAtAsc(
            LocalDate from, LocalDate to);

    List<FinancialMovement> findAllByMovementDateBefore(LocalDate cutoff);
}
