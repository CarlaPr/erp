package com.alfatahi.erp.job;

import com.alfatahi.erp.service.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Atualiza automaticamente o status/semáforo dos agendamentos da Agenda Comercial
 * cujo prazo (Data Aprovação + 15 dias corridos) foi ultrapassado sem conclusão.
 * Segue o mesmo padrão do {@link QuoteExpirationJob} já existente.
 */
@Component
public class ScheduleOverdueJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduleOverdueJob.class);
    private final ScheduleService scheduleService;

    public ScheduleOverdueJob(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void executeOverdueRoutine() {
        log.info("Iniciando varredura noturna da Agenda Comercial (prazos vencidos)...");
        int updated = scheduleService.refreshOverdueStatuses();
        log.info("Varredura concluída! Agendamentos marcados como ATRASADO: {}", updated);
    }
}
