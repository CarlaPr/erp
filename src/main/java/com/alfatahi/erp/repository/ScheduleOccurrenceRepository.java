package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ScheduleOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleOccurrenceRepository extends JpaRepository<ScheduleOccurrence, UUID> {

    List<ScheduleOccurrence> findByScheduleIdOrderByOccurrenceDateAsc(UUID scheduleId);

    Optional<ScheduleOccurrence> findByScheduleIdAndOccurrenceDate(UUID scheduleId, LocalDate occurrenceDate);

    List<ScheduleOccurrence> findByOccurrenceDate(LocalDate occurrenceDate);

    void deleteByScheduleId(UUID scheduleId);
}
