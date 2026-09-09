package com.alfatahi.erp.planocorte.repository;

import com.alfatahi.erp.planocorte.entity.Vidro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VidroRepository extends JpaRepository<Vidro, Long> {

    @EntityGraph(attributePaths = "supplier")
    List<Vidro> findByAtivoTrueOrderByNomeAsc();

    @Override
    @EntityGraph(attributePaths = "supplier")
    List<Vidro> findAll();

    List<Vidro> findByAtivoTrueAndTipoAndEspessuraOrderByNomeAscIdAsc(
            com.alfatahi.erp.planocorte.entity.TipoVidro tipo, java.math.BigDecimal espessura);

    Optional<Vidro> findFirstByNomeIgnoreCase(String nome);
}