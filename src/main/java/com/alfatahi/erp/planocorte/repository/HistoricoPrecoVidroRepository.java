package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.HistoricoPrecoVidro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoPrecoVidroRepository extends JpaRepository<HistoricoPrecoVidro, Long> {

    List<HistoricoPrecoVidro> findByVidroIdOrderByCriadoEmDesc(Long vidroId);
}
