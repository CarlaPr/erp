package com.alfatahi.erp.job;

import com.alfatahi.erp.repository.QuoteRepository;
import com.alfatahi.erp.service.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class QuoteExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpirationJob.class);

    // Orçamentos pendentes viram "expirado" 2 meses após a data de emissão.
    private static final int MESES_PARA_EXPIRAR = 2;

    // Orçamentos já expirados são excluídos definitivamente 6 meses após a data de emissão.
    private static final int MESES_PARA_EXCLUIR = 6;

    private final QuoteRepository quoteRepo;
    private final QuoteService quoteService;

    public QuoteExpirationJob(QuoteRepository quoteRepo, QuoteService quoteService) {
        this.quoteRepo = quoteRepo;
        this.quoteService = quoteService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void executeExpirationRoutine() {

        LocalDateTime expirationLimit = LocalDateTime.now().minusMonths(MESES_PARA_EXPIRAR);

        log.info("Iniciando varredura noturna de orçamentos (Expirando emitidos antes de: {})...", expirationLimit);
        int updatedRows = quoteRepo.expirePendingQuotes(expirationLimit);
        log.info("Varredura concluída com sucesso! Orçamentos expirados: {}", updatedRows);

        LocalDateTime deletionLimit = LocalDateTime.now().minusMonths(MESES_PARA_EXCLUIR);

        log.info("Iniciando exclusão definitiva de orçamentos expirados (emitidos antes de: {})...", deletionLimit);
        int deletedRows = quoteService.purgeExpiredQuotesOlderThan(deletionLimit);
        log.info("Exclusão concluída com sucesso! Orçamentos removidos: {}", deletedRows);
    }
}