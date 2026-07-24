package com.alfatahi.erp.cutplan.repository;

import com.alfatahi.erp.cutplan.entity.CostTableHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CostTableHistoryRepository extends JpaRepository<CostTableHistory, UUID> {

    @Query("SELECT h FROM CostTableHistory h WHERE h.costTable.id = :costTableId ORDER BY h.changedAt DESC")
    List<CostTableHistory> findByCostTableIdOrderByChangedAtDesc(@Param("costTableId") UUID costTableId);
}
