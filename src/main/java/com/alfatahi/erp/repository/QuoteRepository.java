package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<Quote, UUID> {
    // Busca os orçamentos ordenando pelos mais recentes
    List<Quote> findAllByOrderByDateCreatedDesc();
}