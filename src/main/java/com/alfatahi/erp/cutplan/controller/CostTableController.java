package com.alfatahi.erp.cutplan.controller;

import com.alfatahi.erp.cutplan.dto.*;
import com.alfatahi.erp.cutplan.service.CostTableService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CostTableController - API REST para Tabelas de Preço
 *
 * Base URL: /api/v1/cost-tables
 *
 * Endpoints:
 * - GET    /                           → Listar preços (paginado)
 * - GET    /category/{category}        → Listar por categoria
 * - POST   /                            → Criar novo preço
 * - GET    /{id}                        → Obter preço
 * - PUT    /{id}                        → Atualizar preço (cria versão)
 * - DELETE /{id}                        → Deletar preço (soft delete)
 * - POST   /{id}/reactivate             → Reativar preço deletado
 * - GET    /current/{category}/{type}   → Buscar preço vigente
 * - GET    /at-date/{category}/{type}   → Buscar preço em data
 * - GET    /{id}/history                → Histórico de preço
 * - GET    /{id}/analysis               → Análise de variação
 * - GET    /supplier/{supplierId}       → Preços de fornecedor
 * - GET    /expiring-soon               → Preços vencendo em breve
 * - POST   /import-csv                  → Importar de CSV
 * - GET    /export-csv/{category}       → Exportar para CSV
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cost-tables")
public class CostTableController {

    @Autowired
    private CostTableService costTableService;

    /**
     * Listar todos os preços com paginação
     *
     * GET /api/v1/cost-tables
     */
    @GetMapping
        public ResponseEntity<Page<CostTableResponse>> listAll(
            @PageableDefault(size = 20, sort = "effectiveFrom", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Requisição: Listar preços, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<CostTableResponse> page = costTableService.listAll(pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * Listar preços por categoria
     *
     * GET /api/v1/cost-tables?category=GLASS
     */
    @GetMapping(params = "category")
        public ResponseEntity<Page<CostTableResponse>> listByCategory(
                        @RequestParam String category,

            @PageableDefault(size = 20, sort = "itemType")
            Pageable pageable) {

        log.info("Requisição: Listar preços da categoria: {}", category);

        Page<CostTableResponse> page = costTableService.listByCategory(category, pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * Obter preço por ID
     *
     * GET /api/v1/cost-tables/{id}
     */
    @GetMapping("/{id}")
        public ResponseEntity<CostTableResponse> getById(
            @PathVariable
                        UUID id) {

        log.info("Requisição: Obter preço: {}", id);

        CostTableResponse response = costTableService.getById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Buscar preço vigente
     *
     * GET /api/v1/cost-tables/current/GLASS/GLASS_8MM_TRANSPARENTE
     */
    @GetMapping("/current/{category}/{itemType}")
        public ResponseEntity<CostTableResponse> getCurrentPrice(
                        @PathVariable String category,

                        @PathVariable String itemType) {

        log.info("Requisição: Obter preço vigente: {} - {}", category, itemType);

        CostTableResponse response = costTableService.getCurrentPrice(category, itemType);

        return ResponseEntity.ok(response);
    }

    /**
     * Buscar preço para data específica
     *
     * GET /api/v1/cost-tables/at-date/GLASS/GLASS_8MM_TRANSPARENTE?date=2024-03-15
     */
    @GetMapping("/at-date/{category}/{itemType}")
        public ResponseEntity<CostTableResponse> getPriceAtDate(
            @PathVariable String category,
            @PathVariable String itemType,

                        @RequestParam LocalDate date) {

        log.info("Requisição: Obter preço para {}: {} - {} em {}",
                date, category, itemType);

        CostTableResponse response = costTableService.getPriceAtDate(category, itemType, date);

        return ResponseEntity.ok(response);
    }

    /**
     * Criar novo preço
     *
     * POST /api/v1/cost-tables
     */
    @PostMapping
        public ResponseEntity<CostTableResponse> create(
            @Valid
            @RequestBody
            CostTableCreateRequest request,

            @AuthenticationPrincipal User user) {

        log.info("Requisição: Criar novo preço: {} - {}",
                request.getCategory(), request.getItemType());

        CostTableResponse response = costTableService.create(
                request,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Atualizar preço (cria nova versão)
     *
     * PUT /api/v1/cost-tables/{id}
     */
    @PutMapping("/{id}")
        public ResponseEntity<CostTableResponse> updatePrice(
            @PathVariable UUID id,

            @Valid
            @RequestBody
            CostTableUpdateRequest request,

            @AuthenticationPrincipal User user) {

        log.info("Requisição: Atualizar preço: {}", id);

        CostTableResponse response = costTableService.updatePrice(
                id,
                request,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Deletar preço (soft delete)
     *
     * DELETE /api/v1/cost-tables/{id}
     */
    @DeleteMapping("/{id}")
        public ResponseEntity<Void> deletePrice(
            @PathVariable UUID id) {

        log.info("Requisição: Deletar preço: {}", id);

        costTableService.deletePrice(id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    /**
     * Reativar preço deletado
     *
     * POST /api/v1/cost-tables/{id}/reactivate
     */
    @PostMapping("/{id}/reactivate")
        public ResponseEntity<CostTableResponse> reactivatePrice(
            @PathVariable UUID id) {

        log.info("Requisição: Reativar preço: {}", id);

        costTableService.reactivatePrice(id);

        CostTableResponse response = costTableService.getById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Obter histórico de preço
     *
     * GET /api/v1/cost-tables/{id}/history
     */
    @GetMapping("/{id}/history")
        public ResponseEntity<List<CostTableHistoryResponse>> getPriceHistory(
            @PathVariable UUID id) {

        log.info("Requisição: Obter histórico do preço: {}", id);

        List<CostTableHistory> history = costTableService.getPriceChangeHistory(id);

        List<CostTableHistoryResponse> response = history.stream()
                .map(h -> CostTableHistoryResponse.builder()
                        .id(h.getId())
                        .oldPrice(h.getOldPrice())
                        .newPrice(h.getNewPrice())
                        .absoluteDifference(h.getAbsoluteDifference())
                        .percentageDifference(h.getPercentageDifference())
                        .increase(h.isIncrease())
                        .decrease(h.isDecrease())
                        .formattedChange(h.getFormattedChange())
                        .changeDescription(h.getChangeDescription())
                        .reason(h.getReason())
                        .reference(h.getReference())
                        .changedAt(h.getChangedAt())
                        .changedBy(h.getChangedBy().getName())
                        .build())
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Análise de preço
     *
     * GET /api/v1/cost-tables/{id}/analysis
     */
    @GetMapping("/{id}/analysis")
        public ResponseEntity<CostTableAnalysisResponse> getAnalysis(
            @PathVariable UUID id) {

        log.info("Requisição: Análise de preço: {}", id);

        CostTableResponse price = costTableService.getById(id);

        BigDecimal avgVariation = costTableService.getAverageVariationPercent(id);
        BigDecimal maxIncrease = costTableService.getMaxIncreasePercent(id);
        BigDecimal maxDecrease = costTableService.getMaxDecreasePercent(id);

        CostTableAnalysisResponse response = CostTableAnalysisResponse.builder()
                .category(price.getCategory())
                .itemType(price.getDescription())
                .currentPrice(price.getUnitPrice())
                .changeCount((int) costTableService.getPriceChangeHistory(id).size())
                .averageVariationPercent(avgVariation)
                .maxIncreasePercent(maxIncrease)
                .maxDecreasePercent(maxDecrease)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Preços de um fornecedor
     *
     * GET /api/v1/cost-tables/supplier/{supplierId}
     */
    @GetMapping("/supplier/{supplierId}")
        public ResponseEntity<List<CostTableResponse>> getPricesBySupplier(
                        @PathVariable UUID supplierId) {

        log.info("Requisição: Obter preços do fornecedor: {}", supplierId);

        List<CostTableResponse> response = costTableService.getPricesBySupplier(supplierId);

        return ResponseEntity.ok(response);
    }

    /**
     * Preços vencendo em breve
     *
     * GET /api/v1/cost-tables/expiring-soon
     */
    @GetMapping("/expiring-soon")
        public ResponseEntity<List<CostTableResponse>> getSoonToExpire() {

        log.info("Requisição: Preços vencendo em breve");

        List<CostTableResponse> response = costTableService.getSoonToExpire();

        return ResponseEntity.ok(response);
    }

    /**
     * Importar preços de CSV
     *
     * POST /api/v1/cost-tables/import-csv
     * Content-Type: text/csv ou application/octet-stream
     */
    @PostMapping("/import-csv")
        public ResponseEntity<Void> importPricesFromCSV(
            @RequestBody String csvData,
            @AuthenticationPrincipal User user) {

        log.info("Requisição: Importar preços de CSV");

        costTableService.importPricesFromCSV(csvData, UUID.fromString(user.getUsername()));

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .build();
    }

    /**
     * Exportar preços para CSV
     *
     * GET /api/v1/cost-tables/export-csv/GLASS
     */
    @GetMapping("/export-csv/{category}")
        public ResponseEntity<String> exportPricesAsCSV(
                        @PathVariable String category) {

        log.info("Requisição: Exportar preços da categoria: {}", category);

        String csv = costTableService.exportPricesAsCSV(category);

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"precos_" + category + ".csv\"")
                .body(csv);
    }
}