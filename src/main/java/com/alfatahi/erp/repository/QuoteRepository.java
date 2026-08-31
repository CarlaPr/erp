package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Quote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Quote q WHERE q.id = :id")
    Optional<Quote> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "client")
    @Query("SELECT q FROM Quote q WHERE q.status = 'approved' " +
            "AND NOT EXISTS (SELECT w.id FROM WorkOrder w WHERE w.quote = q) " +
            "ORDER BY q.dateApproved DESC, q.dateCreated DESC")
    List<Quote> findApprovedWithoutWorkOrder();

    @Modifying
    @Transactional
    @Query("UPDATE Quote q SET q.status = 'expired' WHERE q.status = 'pending' AND q.dateCreated < :limite")
    int expirePendingQuotes(@Param("limite") LocalDateTime limite);

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