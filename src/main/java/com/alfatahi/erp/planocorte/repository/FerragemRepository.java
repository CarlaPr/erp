package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.Ferragem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FerragemRepository extends JpaRepository<Ferragem, Long> {

    @Override
    @EntityGraph(attributePaths = "supplier")
    List<Ferragem> findAll();
}
