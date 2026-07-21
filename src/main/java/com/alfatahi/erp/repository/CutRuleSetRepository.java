package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutRuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutRuleSetRepository extends JpaRepository<CutRuleSet, UUID> {
    List<CutRuleSet> findByActiveTrueOrderByNameAsc();
}
