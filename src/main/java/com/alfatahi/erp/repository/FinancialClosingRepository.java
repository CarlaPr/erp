package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.FinancialClosing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialClosingRepository extends JpaRepository<FinancialClosing, UUID> {

    List<FinancialClosing> findAllByOrderByPeriodStartDesc();

    Optional<FinancialClosing> findByPeriodStart(LocalDate periodStart);

    /** Retorna o fechamento mais recente, para calcular o saldo de abertura do próximo. */
    @Query("SELECT f FROM FinancialClosing f ORDER BY f.periodStart DESC LIMIT 1")
    Optional<FinancialClosing> findLatestClosing();
}
