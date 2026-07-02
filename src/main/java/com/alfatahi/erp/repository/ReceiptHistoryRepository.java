package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ReceiptHistory;
import com.alfatahi.erp.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReceiptHistoryRepository extends JpaRepository<ReceiptHistory, UUID> {

    /**
     * Buscar histórico de um recibo
     */
    List<ReceiptHistory> findByReceipt(Receipt receipt);

    /**
     * Buscar histórico de um recibo ordenado por data descrescente
     */
    List<ReceiptHistory> findByReceiptOrderByEventDateDesc(Receipt receipt);

    /**
     * Buscar histórico por tipo de evento
     */
    List<ReceiptHistory> findByEventType(String eventType);

    /**
     * Contar eventos de um recibo
     */
    long countByReceipt(Receipt receipt);

    /**
     * Buscar histórico por intervalo de datas
     */
    @Query("SELECT rh FROM ReceiptHistory rh WHERE rh.eventDate BETWEEN :dateFrom AND :dateTo ORDER BY rh.eventDate DESC")
    List<ReceiptHistory> findByDateRange(
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo
    );

    /**
     * Buscar último evento de um recibo
     */
    @Query("SELECT rh FROM ReceiptHistory rh WHERE rh.receipt = :receipt ORDER BY rh.eventDate DESC LIMIT 1")
    ReceiptHistory findLastEventByReceipt(@Param("receipt") Receipt receipt);

    /**
     * Contar eventos de um tipo específico
     */
    long countByEventType(String eventType);

    /**
     * Buscar histórico de um tipo de evento para múltiplos recibos
     */
    @Query("SELECT rh FROM ReceiptHistory rh WHERE rh.receipt IN :receipts AND rh.eventType = :eventType ORDER BY rh.eventDate DESC")
    List<ReceiptHistory> findByReceiptsAndEventType(
            @Param("receipts") List<Receipt> receipts,
            @Param("eventType") String eventType
    );
}
