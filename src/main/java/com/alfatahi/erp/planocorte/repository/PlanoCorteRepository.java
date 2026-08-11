package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.PlanoCorte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoCorteRepository extends JpaRepository<PlanoCorte, Long> {

    List<PlanoCorte> findAllByOrderByIdDesc();
}
