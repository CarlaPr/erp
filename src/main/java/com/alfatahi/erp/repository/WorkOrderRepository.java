package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    List<WorkOrder> findAllByOrderByCreatedAtDesc();

    @Query("SELECT SUM(w.totalValue) FROM WorkOrder w WHERE w.status <> 'cancelled'")
    BigDecimal sumTotalRevenue();

    // 2. Calcula o Custo Total direto no SQL (Ignorando canceladas)
    @Query("SELECT SUM(i.quantity * i.unitCost) FROM WorkOrderItem i WHERE i.workOrder.status <> 'cancelled'")
    BigDecimal sumTotalCost();

    // 3. Busca as Ordens e já traz os Itens na mesma query (Resolve o N+1)
    @Query("SELECT DISTINCT w FROM WorkOrder w LEFT JOIN FETCH w.items ORDER BY w.createdAt DESC")
    List<WorkOrder> findAllWithItemsOrderByCreatedAtDesc();

    @Transactional
    @Query("""
    SELECT COALESCE(MAX(CAST(SUBSTRING(w.number, 4) AS integer)), 1000)
    FROM WorkOrder w WHERE w.number LIKE 'OS-%'
    """)
    Integer findMaxWorkOrderSequence();
}