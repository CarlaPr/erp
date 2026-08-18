package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.TechnicalVisitPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TechnicalVisitPhotoRepository extends JpaRepository<TechnicalVisitPhoto, UUID> {
    List<TechnicalVisitPhoto> findByTechnicalVisitIdOrderByCreatedAtAsc(UUID visitId);
    Optional<TechnicalVisitPhoto> findByIdAndTechnicalVisitId(UUID id, UUID visitId);
    long countByTechnicalVisitId(UUID visitId);
}
