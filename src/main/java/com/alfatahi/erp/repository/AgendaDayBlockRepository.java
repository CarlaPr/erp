package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.AgendaDayBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AgendaDayBlockRepository extends JpaRepository<AgendaDayBlock, LocalDate> {
    List<AgendaDayBlock> findAllByOrderByDateAsc();
}
