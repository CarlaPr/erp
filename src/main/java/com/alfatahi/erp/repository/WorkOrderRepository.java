package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    @Query("SELECT w FROM WorkOrder w WHERE w.quote.id = :quoteId")
    Optional<WorkOrder> findByQuoteId(@Param("quoteId") UUID quoteId);

    List<WorkOrder> findAllByOrderByCreatedAtDesc();

    @Query("SELECT SUM(w.totalValue) FROM WorkOrder w WHERE w.status <> 'cancelled'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT SUM(i.quantity * i.unitCost) FROM WorkOrderItem i WHERE i.workOrder.status <> 'cancelled'")
    BigDecimal sumTotalCost();

    @Query("SELECT DISTINCT w FROM WorkOrder w LEFT JOIN FETCH w.items ORDER BY w.createdAt DESC")
    List<WorkOrder> findAllWithItemsOrderByCreatedAtDesc();

    @Transactional(readOnly = true)
    @Query("""
    SELECT COALESCE(MAX(CAST(SUBSTRING(w.number, 4) AS integer)), 1000)
    FROM WorkOrder w WHERE w.number LIKE 'OS-%'
    """)
    Integer findMaxWorkOrderSequence();

    @Query("SELECT COALESCE(SUM(w.totalValue), 0) FROM WorkOrder w " +
            "WHERE LOWER(w.status) IN ('completed', 'done', 'concluida') " +
            "AND w.installDate >= :inicio AND w.installDate < :fim")
    BigDecimal sumRevenueConcludedByPeriod(@Param("inicio") LocalDate inicio,
                                           @Param("fim") LocalDate fim);
}