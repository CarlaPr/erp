package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.PlanoCorteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoCorteItemRepository extends JpaRepository<PlanoCorteItem, Long> {

    List<PlanoCorteItem> findByPlanoCorteIdOrderByIdAsc(Long planoCorteId);

    Optional<PlanoCorteItem> findByIdAndPlanoCorteId(Long id, Long planoCorteId);
}
