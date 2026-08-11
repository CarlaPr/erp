package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.Acessorio;
import com.alfatahi.erp.planocorte.entity.Silicone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcessorioRepository extends JpaRepository<Acessorio, Long> {

    @Override
    @EntityGraph(attributePaths = "supplier")
    List<Acessorio> findAll();
}
