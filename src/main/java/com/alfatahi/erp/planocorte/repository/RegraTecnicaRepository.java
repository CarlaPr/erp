package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegraTecnicaRepository extends JpaRepository<RegraTecnica, Long> {

    List<RegraTecnica> findByCategoriaAndAtivoTrueOrderByCodigoAsc(CategoriaServico categoria);

    List<RegraTecnica> findAllByOrderByCategoriaAscCodigoAsc();
}
