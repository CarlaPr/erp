package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.TechnicalVisitOpening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicalVisitOpeningRepository extends JpaRepository<TechnicalVisitOpening, UUID> {
    @Query("SELECT DISTINCT o FROM TechnicalVisitOpening o LEFT JOIN FETCH o.features " +
           "WHERE o.technicalVisit.id = :visitId ORDER BY o.createdAt, o.id")
    List<TechnicalVisitOpening> findDetailedByVisitId(@Param("visitId") UUID visitId);

    @Query("SELECT DISTINCT o FROM TechnicalVisitOpening o LEFT JOIN FETCH o.features " +
           "JOIN FETCH o.technicalVisit v JOIN FETCH v.client c WHERE c.id = :clientId ORDER BY v.visitDate DESC, o.createdAt")
    List<TechnicalVisitOpening> findDetailedByClientId(@Param("clientId") UUID clientId);

    Optional<TechnicalVisitOpening> findByIdAndTechnicalVisitId(UUID id, UUID visitId);
    long countByTechnicalVisitId(UUID visitId);
}
