package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ScheduleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleHistoryRepository extends JpaRepository<ScheduleHistory, UUID> {

    List<ScheduleHistory> findByScheduleIdOrderByEventDateAsc(UUID scheduleId);
}
