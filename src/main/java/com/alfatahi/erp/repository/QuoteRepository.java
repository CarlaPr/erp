package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime; // IMPORTANTE: Importar LocalDateTime
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {

    @Modifying
    @Transactional
    @Query("UPDATE Quote q SET q.status = 'expired' WHERE q.status = 'pending' AND q.dateCreated < :limite")
    int expirePendingQuotes(@Param("limite") LocalDateTime limite); // CORRIGIDO PARA LocalDateTime

    @Modifying
    @Transactional
    @Query("""
    SELECT COALESCE(
    MAX(CAST(SUBSTRING(q.number, 5) AS integer)),
    1000
    )
    FROM Quote q
    WHERE q.number LIKE 'ORC-%'
    """)
    Integer findMaxQuoteSequence();
}