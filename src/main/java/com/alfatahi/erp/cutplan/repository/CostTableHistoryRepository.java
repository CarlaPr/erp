package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.CostTableHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CostTableHistoryRepository extends JpaRepository<CostTableHistory, UUID> {

    @Query("SELECT h FROM CostTableHistory h WHERE h.costTable.id = :costTableId ORDER BY h.changedAt DESC")
    List<CostTableHistory> findByCostTableIdOrderByChangedAtDesc(@Param("costTableId") UUID costTableId);

    @Query("SELECT COALESCE(AVG(h.percentageDifference), 0) FROM CostTableHistory h WHERE h.costTable.id = :costTableId")
    BigDecimal getAverageVariationPercent(@Param("costTableId") UUID costTableId);

    @Query("SELECT COALESCE(MAX(h.percentageDifference), 0) FROM CostTableHistory h WHERE h.costTable.id = :costTableId AND h.increase = true")
    BigDecimal getMaxIncreasePercent(@Param("costTableId") UUID costTableId);

    @Query("SELECT COALESCE(MAX(h.percentageDifference), 0) FROM CostTableHistory h WHERE h.costTable.id = :costTableId AND h.decrease = true")
    BigDecimal getMaxDecreasePercent(@Param("costTableId") UUID costTableId);
}