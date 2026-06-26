package com.alfatahi.erp.job;

import com.alfatahi.erp.repository.QuoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime; // IMPORTANTE: Importar LocalDateTime

@Component
public class QuoteExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpirationJob.class);
    private final QuoteRepository quoteRepo;

    public QuoteExpirationJob(QuoteRepository quoteRepo) {
        this.quoteRepo = quoteRepo;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void executeExpirationRoutine() {

        LocalDateTime limitDate = LocalDateTime.now().minusMonths(1);

        log.info("Iniciando varredura noturna de orçamentos (Expirando anteriores a: {})...", limitDate);
        int updatedRows = quoteRepo.expirePendingQuotes(limitDate);
        log.info("Varredura concluída com sucesso! Orçamentos expirados: {}", updatedRows);
    }
}