package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    @Query("SELECT w FROM WorkOrder w WHERE w.quote.id = :quoteId")
    Optional<WorkOrder> findByQuoteId(@Param("quoteId") UUID quoteId);

    List<WorkOrder> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT DISTINCT w FROM WorkOrder w
            LEFT JOIN FETCH w.client LEFT JOIN FETCH w.category LEFT JOIN FETCH w.quote LEFT JOIN FETCH w.items
            WHERE (:history = true AND LOWER(TRIM(COALESCE(w.status, ''))) IN :completed)
               OR (:history = false AND LOWER(TRIM(COALESCE(w.status, ''))) NOT IN :closed)
            ORDER BY w.createdAt DESC
            """)
    List<WorkOrder> findTechnicianListing(boolean history, Set<String> completed, Set<String> closed);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WorkOrder w WHERE w.id = :id")
    Optional<WorkOrder> findByIdForFieldUpdate(UUID id);

    @Query("SELECT SUM(w.totalValue) FROM WorkOrder w WHERE w.status <> 'cancelled'")
    BigDecimal sumTotalRevenue();

    @Query("SELECT SUM(i.quantity * i.unitCost) FROM WorkOrderItem i WHERE i.workOrder.status <> 'cancelled'")
    BigDecimal sumTotalCost();

    @Query("SELECT DISTINCT w FROM WorkOrder w LEFT JOIN FETCH w.items ORDER BY w.createdAt DESC")
    List<WorkOrder> findAllWithItemsOrderByCreatedAtDesc();

    @Query("""
            SELECT DISTINCT w FROM WorkOrder w LEFT JOIN FETCH w.items
            WHERE (w.createdAt >= :from AND w.createdAt < :to)
               OR (w.createdAt < :from AND LOWER(TRIM(COALESCE(w.status, ''))) NOT IN
                   ('completed', 'delivered', 'done', 'concluida', 'concluída', 'cancelled', 'canceled'))
            ORDER BY w.createdAt DESC
            """)
    List<WorkOrder> findListingByPeriod(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Transactional(readOnly = true)
    @Query("""
    SELECT COALESCE(MAX(CAST(SUBSTRING(w.number, 4) AS integer)), 1000)
    FROM WorkOrder w WHERE w.number LIKE 'OS-%'
    """)
    Integer findMaxWorkOrderSequence();

    @Query("SELECT COALESCE(SUM(w.totalValue), 0) FROM WorkOrder w " +
            "WHERE LOWER(w.status) IN ('completed', 'done', 'delivered', 'concluida') " +
            "AND w.installDate >= :inicio AND w.installDate < :fim")
    BigDecimal sumRevenueConcludedByPeriod(@Param("inicio") LocalDate inicio,
                                           @Param("fim") LocalDate fim);
}
