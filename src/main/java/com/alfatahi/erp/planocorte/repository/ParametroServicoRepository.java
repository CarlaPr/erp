package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ParametroServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametroServicoRepository extends JpaRepository<ParametroServico, Long> {

    List<ParametroServico> findAllByOrderByCategoriaAscCodigoAsc();

    Optional<ParametroServico> findByCategoriaAndCodigoAndAtivoTrue(CategoriaServico categoria, String codigo);
}
