package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Loss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LossRepository extends JpaRepository<Loss, UUID> {
    List<Loss> findAllByOrderByOccurrenceDateDesc();

    @Query("SELECT COALESCE(SUM(l.financialImpact), 0) FROM Loss l " +
            "WHERE l.occurrenceDate >= :inicio AND l.occurrenceDate < :fim")
    BigDecimal sumFinancialImpactByPeriod(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}