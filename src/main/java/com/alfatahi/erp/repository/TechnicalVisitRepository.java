package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.TechnicalVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TechnicalVisitRepository extends JpaRepository<TechnicalVisit, UUID> {

    List<TechnicalVisit> findByQuoteId(UUID quoteId);

    List<TechnicalVisit> findByClientIdOrderByVisitDateDescVisitTimeDesc(UUID clientId);

    @Query("SELECT DISTINCT v FROM TechnicalVisit v " +
           "LEFT JOIN FETCH v.quote q " +
           "LEFT JOIN FETCH v.client " +
           "ORDER BY v.visitDate ASC, v.visitTime ASC")
    List<TechnicalVisit> findAllWithRelations();
}
