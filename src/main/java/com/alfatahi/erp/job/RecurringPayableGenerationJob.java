package com.alfatahi.erp.job;

import com.alfatahi.erp.service.RecurrenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mantém as séries de Contas Recorrentes "abastecidas": gera as próximas parcelas
 * de recorrências com quantidade/data definidas até completá-las, e mantém um
 * horizonte mínimo de parcelas futuras para recorrências infinitas.
 * Segue o mesmo padrão dos demais jobs noturnos do sistema (ex.: {@link ScheduleOverdueJob}).
 */
@Component
public class RecurringPayableGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringPayableGenerationJob.class);
    private final RecurrenceService recurrenceService;

    public RecurringPayableGenerationJob(RecurrenceService recurrenceService) {
        this.recurrenceService = recurrenceService;
    }

    @Scheduled(cron = "0 15 0 * * ?")
    public void executeTopUp() {
        log.info("Iniciando geração automática de parcelas de Contas Recorrentes...");
        int generated = recurrenceService.topUpAllActiveRecurrences();
        log.info("Geração concluída! Novas parcelas criadas: {}", generated);
    }
}
