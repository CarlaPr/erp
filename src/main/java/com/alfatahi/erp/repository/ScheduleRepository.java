package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    Optional<Schedule> findByQuoteId(UUID quoteId);

    Optional<Schedule> findByWorkOrderId(UUID workOrderId);

    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.quote q " +
           "LEFT JOIN FETCH q.items " +
           "LEFT JOIN FETCH s.workOrder wo " +
           "LEFT JOIN FETCH wo.category " +
           "LEFT JOIN FETCH s.client " +
           "ORDER BY s.approvalDate DESC")
    List<Schedule> findAllWithRelations();
}
