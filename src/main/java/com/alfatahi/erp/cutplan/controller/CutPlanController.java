package com.alfatahi.erp.cutplan.controller;

import com.alfatahi.erp.cutplan.dto.*;
import com.alfatahi.erp.cutplan.service.*;
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

import java.util.List;
import java.util.UUID;

/**
 * CutPlanController - API REST para Planos de Corte
 *
 * Base URL: /api/v1/cut-plans
 *
 * Endpoints:
 * - POST   /work-orders/{woId}/generate     → Criar plano de WorkOrder
 * - GET    /                                 → Listar planos (paginado)
 * - GET    /{id}                             → Obter plano por ID
 * - GET    /{id}/detailed                    → Obter plano com histórico
 * - GET    /work-orders/{woId}               → Obter plano de uma WorkOrder
 * - GET    /{id}/statistics                  → Obter estatísticas
 * - GET    /{id}/history                     → Obter histórico
 * - POST   /{id}/items                       → Adicionar item
 * - DELETE /{id}/items/{itemId}              → Remover item
 * - PUT    /{id}                             → Atualizar plano
 * - POST   /{id}/approve                     → Aprovar plano
 * - POST   /{id}/send-to-supplier            → Enviar para fornecedor
 * - POST   /{id}/cancel                      → Cancelar plano
 * - POST   /{id}/recalculate-costs           → Recalcular custos
 * - DELETE /{id}                             → Deletar plano
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cut-plans")
public class CutPlanController {

    @Autowired
    private CutPlanService cutPlanService;

    @Autowired
    private TechnicalRulesService technicalRulesService;

    @Autowired
    private CostCalculatorService costCalculatorService;

    @Autowired
    private CutPlanHistoryService cutPlanHistoryService;

    /**
     * Criar novo plano de corte a partir de uma Ordem de Serviço
     *
     * POST /api/v1/cut-plans/work-orders/{woId}/generate
     */
    @PostMapping("/work-orders/{woId}/generate")
                    public ResponseEntity<CutPlanResponse> generateFromWorkOrder(
            @PathVariable
                        UUID woId,

            @AuthenticationPrincipal User user) {

        log.info("Requisição: Gerar plano de corte para WorkOrder: {}", woId);

        CutPlanResponse response = cutPlanService.createFromWorkOrder(
                woId,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Listar todos os planos de corte com paginação
     *
     * GET /api/v1/cut-plans
     */
    @GetMapping
            public ResponseEntity<Page<CutPlanListResponse>> listAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Requisição: Listar planos, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<CutPlanListResponse> page = cutPlanService.listAll(pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * Listar planos filtrados por status
     *
     * GET /api/v1/cut-plans?status=DRAFT
     */
    @GetMapping(params = "status")
        public ResponseEntity<Page<CutPlanListResponse>> listByStatus(
                        @RequestParam String status,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.info("Requisição: Listar planos por status: {}", status);

        Page<CutPlanListResponse> page = cutPlanService.listByStatus(status, pageable);

        return ResponseEntity.ok(page);
    }

    /**
     * Obter plano por ID (simples)
     *
     * GET /api/v1/cut-plans/{id}
     */
    @GetMapping("/{id}")
                public ResponseEntity<CutPlanResponse> getById(
            @PathVariable
                        UUID id) {

        log.info("Requisição: Obter plano: {}", id);

        CutPlanResponse response = cutPlanService.getById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Obter plano detalhado com histórico completo
     *
     * GET /api/v1/cut-plans/{id}/detailed
     */
    @GetMapping("/{id}/detailed")
        public ResponseEntity<CutPlanDetailedResponse> getByIdDetailed(
            @PathVariable UUID id) {

        log.info("Requisição: Obter plano detalhado: {}", id);

        CutPlanDetailedResponse response = cutPlanService.getByIdWithHistory(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Obter plano de uma Ordem de Serviço específica
     *
     * GET /api/v1/cut-plans/work-orders/{woId}
     */
    @GetMapping("/work-orders/{woId}")
        public ResponseEntity<CutPlanResponse> getByWorkOrderId(
            @PathVariable UUID woId) {

        log.info("Requisição: Obter plano para WorkOrder: {}", woId);

        CutPlanResponse response = cutPlanService.getByWorkOrderId(woId);

        return ResponseEntity.ok(response);
    }

    /**
     * Obter estatísticas completas do plano
     *
     * GET /api/v1/cut-plans/{id}/statistics
     */
    @GetMapping("/{id}/statistics")
        public ResponseEntity<CutPlanStatisticsResponse> getStatistics(
            @PathVariable UUID id) {

        log.info("Requisição: Obter estatísticas do plano: {}", id);

        CutPlanStatisticsResponse response = cutPlanService.getStatistics(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Obter histórico de mudanças do plano
     *
     * GET /api/v1/cut-plans/{id}/history
     */
    @GetMapping("/{id}/history")
        public ResponseEntity<List<CutPlanHistoryResponse>> getHistory(
            @PathVariable UUID id) {

        log.info("Requisição: Obter histórico do plano: {}", id);

        List<CutPlanHistoryResponse> history = cutPlanService.getHistory(id).stream()
                .map(h -> CutPlanHistoryResponse.builder()
                        .id(h.getId())
                        .changeType(h.getChangeType())
                        .changeTypeLabel(h.getChangeTypeLabel())
                        .description(h.getDescription())
                        .version(h.getVersion())
                        .changedAt(h.getChangedAt())
                        .changedBy(h.getChangedBy().getName())
                        .affectedItemId(h.getAffectedItemId())
                        .affectedItemDescription(h.getAffectedItemDescription())
                        .build())
                .toList();

        return ResponseEntity.ok(history);
    }

    /**
     * Atualizar plano
     *
     * PUT /api/v1/cut-plans/{id}
     */
    @PutMapping("/{id}")
                    public ResponseEntity<CutPlanResponse> update(
            @PathVariable UUID id,

            @Valid
            @RequestBody
            CutPlanUpdateRequest request,

            @AuthenticationPrincipal User user) {

        log.info("Requisição: Atualizar plano: {}", id);

        CutPlanResponse response = cutPlanService.update(
                id,
                request,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Adicionar item ao plano
     *
     * POST /api/v1/cut-plans/{id}/items
     */
    @PostMapping("/{id}/items")
                public ResponseEntity<CutPlanResponse> addItem(
            @PathVariable UUID id,

            @Valid
            @RequestBody
            CutPlanItemRequest itemRequest,

            @AuthenticationPrincipal User user) {

        log.info("Requisição: Adicionar item ao plano: {}", id);

        CutPlanResponse response = cutPlanService.addItem(
                id,
                itemRequest,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Remover item do plano
     *
     * DELETE /api/v1/cut-plans/{id}/items/{itemId}
     */
    @DeleteMapping("/{id}/items/{itemId}")
        public ResponseEntity<CutPlanResponse> removeItem(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal User user) {

        log.info("Requisição: Remover item {} do plano: {}", itemId, id);

        CutPlanResponse response = cutPlanService.removeItem(
                id,
                itemId,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Aprovar plano
     *
     * POST /api/v1/cut-plans/{id}/approve
     */
    @PostMapping("/{id}/approve")
                public ResponseEntity<CutPlanResponse> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        log.info("Requisição: Aprovar plano: {}", id);

        CutPlanResponse response = cutPlanService.approvePlan(
                id,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Enviar plano para fornecedor
     *
     * POST /api/v1/cut-plans/{id}/send-to-supplier
     */
    @PostMapping("/{id}/send-to-supplier")
                public ResponseEntity<CutPlanResponse> sendToSupplier(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        log.info("Requisição: Enviar plano para fornecedor: {}", id);

        CutPlanResponse response = cutPlanService.sendToSupplier(
                id,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Cancelar plano
     *
     * POST /api/v1/cut-plans/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
        public ResponseEntity<CutPlanResponse> cancel(
            @PathVariable UUID id,

                        @RequestParam String reason,

            @AuthenticationPrincipal User user) {

        log.info("Requisição: Cancelar plano: {}, Motivo: {}", id, reason);

        CutPlanResponse response = cutPlanService.cancelPlan(
                id,
                reason,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }


    /**
     * Recalcular custos
     *
     * POST /api/v1/cut-plans/{id}/recalculate-costs
     */
    @PostMapping("/{id}/recalculate-costs")
        public ResponseEntity<CutPlanResponse> recalculateCosts(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        log.info("Requisição: Recalcular custos do plano: {}", id);

        CutPlanResponse response = cutPlanService.recalculateCosts(
                id,
                UUID.fromString(user.getUsername())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Deletar plano
     *
     * DELETE /api/v1/cut-plans/{id}
     */
    @DeleteMapping("/{id}")
                public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        log.info("Requisição: Deletar plano: {}", id);

        cutPlanService.deletePlan(id, UUID.fromString(user.getUsername()));

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}