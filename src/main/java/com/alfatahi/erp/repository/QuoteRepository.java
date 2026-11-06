package com.alfatahi.erp.repository;

import com.alfatahi.erp.dto.QuoteListItem;
import com.alfatahi.erp.entity.Quote;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    @Query(value = """
            SELECT new com.alfatahi.erp.dto.QuoteListItem(
                q.id, q.number, q.status, q.dateCreated, c.name, w.id, w.number, q.publicToken,
                CASE WHEN q.clientSignature IS NOT NULL AND q.clientSignature <> '' THEN true ELSE false END
            )
            FROM Quote q
            LEFT JOIN q.client c
            LEFT JOIN q.workOrder w
            WHERE (:status = '' OR q.status = :status)
              AND (:number = '' OR LOWER(q.number) LIKE LOWER(CONCAT('%', :number, '%')))
              AND (:name = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:allDates = true OR (q.dateCreated >= :fromDate AND q.dateCreated < :toDate))
            ORDER BY q.dateCreated DESC NULLS LAST, q.number DESC
            """,
            countQuery = """
            SELECT COUNT(q) FROM Quote q
            LEFT JOIN q.client c
            WHERE (:status = '' OR q.status = :status)
              AND (:number = '' OR LOWER(q.number) LIKE LOWER(CONCAT('%', :number, '%')))
              AND (:name = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:allDates = true OR (q.dateCreated >= :fromDate AND q.dateCreated < :toDate))
            """)
    Page<QuoteListItem> findPageByCreatedDate(@Param("status") String status,
                                              @Param("number") String number,
                                              @Param("name") String name,
                                              @Param("allDates") boolean allDates,
                                              @Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate,
                                              Pageable pageable);

    @Query(value = """
            SELECT new com.alfatahi.erp.dto.QuoteListItem(
                q.id, q.number, q.status, q.dateCreated, c.name, w.id, w.number, q.publicToken,
                CASE WHEN q.clientSignature IS NOT NULL AND q.clientSignature <> '' THEN true ELSE false END
            )
            FROM Quote q
            LEFT JOIN q.client c
            LEFT JOIN q.workOrder w
            WHERE (:status = '' OR q.status = :status)
              AND (:number = '' OR LOWER(q.number) LIKE LOWER(CONCAT('%', :number, '%')))
              AND (:name = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:allDates = true OR (q.dateApproved >= :fromDate AND q.dateApproved < :toDate))
            ORDER BY q.dateApproved DESC NULLS LAST, q.dateCreated DESC NULLS LAST, q.number DESC
            """,
            countQuery = """
            SELECT COUNT(q) FROM Quote q
            LEFT JOIN q.client c
            WHERE (:status = '' OR q.status = :status)
              AND (:number = '' OR LOWER(q.number) LIKE LOWER(CONCAT('%', :number, '%')))
              AND (:name = '' OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :name, '%')))
              AND (:allDates = true OR (q.dateApproved >= :fromDate AND q.dateApproved < :toDate))
            """)
    Page<QuoteListItem> findPageByApprovedDate(@Param("status") String status,
                                               @Param("number") String number,
                                               @Param("name") String name,
                                               @Param("allDates") boolean allDates,
                                               @Param("fromDate") LocalDateTime fromDate,
                                               @Param("toDate") LocalDateTime toDate,
                                               Pageable pageable);

    @Query(value = """
            SELECT month_reference FROM (
                SELECT TO_CHAR(date_created - INTERVAL '5 days', 'YYYY-MM') AS month_reference
                FROM quotes WHERE date_created IS NOT NULL
                UNION
                SELECT TO_CHAR(date_approved - INTERVAL '5 days', 'YYYY-MM') AS month_reference
                FROM quotes WHERE date_approved IS NOT NULL
            ) quote_months
            ORDER BY month_reference DESC
            """, nativeQuery = true)
    List<String> findAvailableMonthReferences();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quote q WHERE q.id = :id")
    Optional<Quote> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "client")
    @Query("SELECT q FROM Quote q WHERE q.status = 'approved' " +
            "AND NOT EXISTS (SELECT w.id FROM WorkOrder w WHERE w.quote = q) " +
            "ORDER BY q.dateApproved DESC, q.dateCreated DESC")
    List<Quote> findApprovedWithoutWorkOrder();

    /**
     * Marca como "expired" os orçamentos pendentes cuja data de validade já passou. A validade é
     * inclusiva: um orçamento válido até 10/05 continua válido durante todo o dia 10/05 e só
     * expira a partir do dia 11/05 (ou seja, quando {@code validUntil < hoje}).
     */
    @Modifying
    @Transactional
    @Query("UPDATE Quote q SET q.status = 'expired' WHERE q.status = 'pending' AND q.validUntil IS NOT NULL AND q.validUntil < :hoje")
    int expirePendingQuotes(@Param("hoje") LocalDate hoje);

    @Query("SELECT q FROM Quote q WHERE q.status = 'expired' AND q.dateCreated < :limite")
    List<Quote> findExpiredQuotesOlderThan(@Param("limite") LocalDateTime limite);

    @Query("""
    SELECT COALESCE(
    MAX(CAST(SUBSTRING(q.number, 5) AS integer)),
    1000
    )
    FROM Quote q
    WHERE q.number LIKE 'ORC-%'
    """)
    Integer findMaxQuoteSequence();

    @Query("SELECT q.status, COUNT(q) FROM Quote q GROUP BY q.status")
    List<Object[]> countByStatus();

    @EntityGraph(attributePaths = {"items", "client"})
    Optional<Quote> findByPublicToken(String token);
}
