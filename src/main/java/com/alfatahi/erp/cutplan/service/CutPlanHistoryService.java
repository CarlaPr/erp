package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.entity.CutPlan;
import com.alfatahi.erp.cutplan.entity.CutPlanHistory;
import com.alfatahi.erp.cutplan.repository.CutPlanHistoryRepository;
import com.alfatahi.erp.entity.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class CutPlanHistoryService {

    @Autowired
    private CutPlanHistoryRepository cutPlanHistoryRepository;

    /**
     * Método principal para registrar mudanças
     */
    public void recordChange(CutPlan cutPlan, AppUser changedBy, String changeType,
                             String description, String oldValues, String newValues) {
        CutPlanHistory history = CutPlanHistory.builder()
                .cutPlan(cutPlan)
                .changedBy(changedBy)
                .changeType(changeType)
                .description(description)
                .version(cutPlan.getVersion())
                .oldValues(oldValues)
                .newValues(newValues)
                .build();

        cutPlanHistoryRepository.save(history);
        log.debug("Histórico registrado: {} | {}", changeType, description);
    }

    public List<CutPlanHistory> getHistory(UUID cutPlanId) {
        return cutPlanHistoryRepository.findByCutPlanIdOrderByChangedAtDesc(cutPlanId);
    }

    public long countChanges(UUID cutPlanId) {
        return cutPlanHistoryRepository.countByCutPlanId(cutPlanId);
    }
}
