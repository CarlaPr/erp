package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.PerfilAluminio;
import com.alfatahi.erp.planocorte.entity.Silicone;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SiliconeRepository extends JpaRepository<Silicone, Long> {

    @Override
    @EntityGraph(attributePaths = "supplier")
    List<Silicone> findAll();
}
