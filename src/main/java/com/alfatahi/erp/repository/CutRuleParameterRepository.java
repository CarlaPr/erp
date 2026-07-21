package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.CutRuleParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CutRuleParameterRepository extends JpaRepository<CutRuleParameter, UUID> {
    List<CutRuleParameter> findByRuleSetIdOrderBySortOrderAsc(UUID ruleSetId);
}
