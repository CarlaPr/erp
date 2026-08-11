package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.Ferragem;
import com.alfatahi.erp.planocorte.entity.PerfilAluminio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerfilAluminioRepository extends JpaRepository<PerfilAluminio, Long> {

    @Override
    @EntityGraph(attributePaths = "supplier")
    List<PerfilAluminio> findAll();
}
