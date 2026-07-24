package com.alfatahi.erp.cutplan.service;

import com.alfatahi.erp.cutplan.dto.*;
import com.alfatahi.erp.cutplan.entity.CutPlan;
import com.alfatahi.erp.cutplan.entity.CutPlanHistory;
import com.alfatahi.erp.cutplan.entity.CutPlanItem;
import com.alfatahi.erp.cutplan.repository.CutPlanItemRepository;
import com.alfatahi.erp.cutplan.repository.CutPlanRepository;
import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.entity.ServiceCategory;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CutPlanService {

    @Autowired
    private CutPlanRepository cutPlanRepository;

    @Autowired
    private CutPlanItemRepository cutPlanItemRepository;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TechnicalRulesService technicalRulesService;

    @Autowired
    private CutPlanHistoryService cutPlanHistoryService;

    // ──────────────────────────────────────────────────
    // CRIAÇÃO
    // ──────────────────────────────────────────────────

    public CutPlanResponse createFromWorkOrder(UUID workOrderId, UUID userId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new EntityNotFoundException("OS não encontrada: " + workOrderId));

        if (cutPlanRepository.existsByWorkOrderId(workOrderId)) {
            throw new IllegalStateException("Já existe um plano de corte para esta OS: " + workOrderId);
        }

        AppUser user = appUserRepository.findById(userId).orElse(null);

        CutPlan plan = CutPlan.builder()
                .workOrder(wo)
                .status("DRAFT")
                .version(1)
                .description("Plano de corte gerado automaticamente da OS #" + wo.getNumber())
                .createdBy(user)
                .updatedBy(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        plan = cutPlanRepository.save(plan);

        // Gerar itens a partir dos itens da OS
        if (wo.getItems() != null && !wo.getItems().isEmpty()) {
            for (var woItem : wo.getItems()) {
                int qty = woItem.getQuantity() != null ? woItem.getQuantity().intValue() : 1;
                CutPlanItem item = CutPlanItem.builder()
                        .cutPlan(plan)
                        .description(woItem.getDescription() != null ? woItem.getDescription() : "Item")
                        .glassType("TEMPERADO")
                        .thickness(new BigDecimal("8"))
                        .color("TRANSPARENTE")
                        .grossWidth(BigDecimal.ZERO)
                        .grossHeight(BigDecimal.ZERO)
                        .finalWidth(BigDecimal.ZERO)
                        .finalHeight(BigDecimal.ZERO)
                        .quantity(qty)
                        .build();

                // Aplicar regras técnicas se categoria disponível
                if (wo.getCategory() != null) {
                    try {
                        technicalRulesService.applyRules(item, wo.getCategory());
                    } catch (Exception e) {
                        log.warn("Não foi possível aplicar regras para item: {}", e.getMessage());
                    }
                }

                recalculateArea(item);
                plan.getItems().add(item);
                cutPlanItemRepository.save(item);
            }
        }

        cutPlanHistoryService.recordChange(plan, user, "CREATED",
                "Plano criado a partir da OS #" + wo.getNumber(), null, null);

        log.info("Plano de corte criado: {} para OS: {}", plan.getId(), workOrderId);
        return toResponse(plan);
    }

    // ──────────────────────────────────────────────────
    // LISTAGEM
    // ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CutPlanListResponse> listAll(Pageable pageable) {
        Page<CutPlan> page = cutPlanRepository.findAll(pageable);
        return page.map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public Page<CutPlanListResponse> listByStatus(String status, Pageable pageable) {
        List<CutPlan> all = cutPlanRepository.findAll();
        List<CutPlan> filtered = all.stream()
                .filter(p -> status.equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<CutPlan> pageContent = start >= filtered.size() ? new ArrayList<>() : filtered.subList(start, end);
        return new PageImpl<>(pageContent.stream().map(this::toListResponse).collect(Collectors.toList()), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public CutPlanResponse getById(UUID id) {
        CutPlan plan = findPlan(id);
        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public CutPlanDetailedResponse getDetailedById(UUID id) {
        CutPlan plan = findPlan(id);
        CutPlanDetailedResponse resp = new CutPlanDetailedResponse();
        resp.setId(plan.getId());
        resp.setWorkOrderId(plan.getWorkOrder() != null ? plan.getWorkOrder().getId() : null);
        resp.setStatus(plan.getStatus());
        resp.setVersion(plan.getVersion());
        resp.setDescription(plan.getDescription());
        resp.setItems(plan.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()));
        resp.setCreatedAt(plan.getCreatedAt());
        resp.setUpdatedAt(plan.getUpdatedAt());
        return resp;
    }

    @Transactional(readOnly = true)
    public CutPlanResponse getByWorkOrderId(UUID workOrderId) {
        CutPlan plan = cutPlanRepository.findByWorkOrderId(workOrderId)
                .orElseThrow(() -> new EntityNotFoundException("Plano não encontrado para OS: " + workOrderId));
        return toResponse(plan);
    }

    // ──────────────────────────────────────────────────
    // ITENS
    // ──────────────────────────────────────────────────

    public CutPlanResponse addItem(UUID planId, CutPlanItemRequest request, UUID userId) {
        CutPlan plan = findPlan(planId);
        assertDraft(plan);

        AppUser user = appUserRepository.findById(userId).orElse(null);

        CutPlanItem item = CutPlanItem.builder()
                .cutPlan(plan)
                .description(request.getDescription())
                .environment(request.getEnvironment())
                .glassType(request.getGlassType() != null ? request.getGlassType() : "TEMPERADO")
                .thickness(request.getThickness() != null ? request.getThickness() : new BigDecimal("8"))
                .color(request.getColor() != null ? request.getColor() : "TRANSPARENTE")
                .finishing(request.getFinishing())
                .grossWidth(request.getGrossWidth())
                .grossHeight(request.getGrossHeight())
                .finalWidth(request.getFinalWidth() != null ? request.getFinalWidth() : request.getGrossWidth())
                .finalHeight(request.getFinalHeight() != null ? request.getFinalHeight() : request.getGrossHeight())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .notes(request.getNotes())
                .angle(request.getAngle())
                .drillingDiameter(request.getDrillingDiameter())
                .drillingQuantity(request.getDrillingQuantity())
                .drillingCostPerUnit(request.getDrillingCostPerUnit())
                .notchDescription(request.getNotchDescription())
                .notchCost(request.getNotchCost())
                .build();

        if (request.getSupplierId() != null) {
            supplierRepository.findById(request.getSupplierId()).ifPresent(item::setSupplier);
        }

        recalculateArea(item);
        cutPlanItemRepository.save(item);
        plan.getItems().add(item);
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy(user);
        cutPlanRepository.save(plan);

        cutPlanHistoryService.recordChange(plan, user, "ITEM_ADDED",
                "Item adicionado: " + item.getDescription(), null, null);

        return toResponse(plan);
    }

    public CutPlanResponse removeItem(UUID planId, UUID itemId, UUID userId) {
        CutPlan plan = findPlan(planId);
        assertDraft(plan);

        AppUser user = appUserRepository.findById(userId).orElse(null);

        CutPlanItem item = plan.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Item não encontrado: " + itemId));

        String desc = item.getDescription();
        plan.getItems().remove(item);
        cutPlanItemRepository.delete(item);
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy(user);
        cutPlanRepository.save(plan);

        cutPlanHistoryService.recordChange(plan, user, "ITEM_REMOVED",
                "Item removido: " + desc, null, null);

        return toResponse(plan);
    }

    // ──────────────────────────────────────────────────
    // STATUS
    // ──────────────────────────────────────────────────

    public CutPlanResponse approvePlan(UUID planId, UUID userId) {
        CutPlan plan = findPlan(planId);
        assertDraft(plan);
        AppUser user = appUserRepository.findById(userId).orElse(null);

        String oldStatus = plan.getStatus();
        plan.setStatus("APPROVED");
        plan.setVersion(plan.getVersion() + 1);
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy(user);
        cutPlanRepository.save(plan);

        cutPlanHistoryService.recordChange(plan, user, "APPROVED",
                "Status alterado de " + oldStatus + " para APPROVED", oldStatus, "APPROVED");

        return toResponse(plan);
    }

    public CutPlanResponse sendToSupplier(UUID planId, UUID userId) {
        CutPlan plan = findPlan(planId);
        if (!"APPROVED".equals(plan.getStatus())) {
            throw new IllegalStateException("Plano deve estar APPROVED para enviar ao fornecedor");
        }
        AppUser user = appUserRepository.findById(userId).orElse(null);

        plan.setStatus("SENT_TO_SUPPLIER");
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy(user);
        cutPlanRepository.save(plan);

        cutPlanHistoryService.recordChange(plan, user, "SENT_TO_SUPPLIER",
                "Plano enviado para fornecedor", "APPROVED", "SENT_TO_SUPPLIER");

        return toResponse(plan);
    }

    public CutPlanResponse cancelPlan(UUID planId, String reason, UUID userId) {
        CutPlan plan = findPlan(planId);
        AppUser user = appUserRepository.findById(userId).orElse(null);

        plan.setStatus("CANCELLED");
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy(user);
        cutPlanRepository.save(plan);

        cutPlanHistoryService.recordChange(plan, user, "CANCELLED",
                "Plano cancelado. Motivo: " + reason, null, "CANCELLED");

        return toResponse(plan);
    }

    public CutPlanResponse recalculateCosts(UUID planId, UUID userId) {
        CutPlan plan = findPlan(planId);
        AppUser user = appUserRepository.findById(userId).orElse(null);

        // Recalcular áreas de todos os itens
        for (CutPlanItem item : plan.getItems()) {
            recalculateArea(item);
            cutPlanItemRepository.save(item);
        }

        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy(user);
        cutPlanRepository.save(plan);

        cutPlanHistoryService.recordChange(plan, user, "COSTS_RECALCULATED",
                "Custos recalculados para " + plan.getItems().size() + " itens", null, null);

        return toResponse(plan);
    }

    public CutPlanOptimizationResultResponse optimizeLayout(UUID planId, UUID userId) {
        // Optimization removed as per requirements - return empty response
        CutPlanOptimizationResultResponse resp = new CutPlanOptimizationResultResponse();
        return resp;
    }

    public void deletePlan(UUID planId, UUID userId) {
        CutPlan plan = findPlan(planId);
        if (!"DRAFT".equals(plan.getStatus()) && !"CANCELLED".equals(plan.getStatus())) {
            throw new IllegalStateException("Plano só pode ser deletado em status DRAFT ou CANCELLED");
        }
        cutPlanRepository.delete(plan);
    }

    // ──────────────────────────────────────────────────
    // ESTATÍSTICAS
    // ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CutPlanStatisticsResponse getStatistics(UUID planId) {
        CutPlan plan = findPlan(planId);
        CutPlanStatisticsResponse stats = new CutPlanStatisticsResponse();
        stats.setPlanId(planId);
        stats.setTotalItems(plan.getItems().size());
        stats.setTotalQuantity(plan.getItems().stream().mapToInt(CutPlanItem::getQuantity).sum());
        stats.setTotalArea(plan.getItems().stream()
                .map(i -> i.getCalculatedArea() != null ? i.getCalculatedArea() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stats.setStatus(plan.getStatus());
        return stats;
    }

    @Transactional(readOnly = true)
    public List<CutPlanHistoryResponse> getHistory(UUID planId) {
        CutPlan plan = findPlan(planId);
        return plan.getHistory().stream().map(h -> {
            CutPlanHistoryResponse r = new CutPlanHistoryResponse();
            r.setId(h.getId());
            r.setChangeType(h.getChangeType());
            r.setDescription(h.getDescription());
            r.setChangedAt(h.getChangedAt());
            r.setChangedBy(h.getChangedBy() != null ? h.getChangedBy().getUsername() : "sistema");
            return r;
        }).collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────
    // HELPERS INTERNOS
    // ──────────────────────────────────────────────────

    private CutPlan findPlan(UUID id) {
        return cutPlanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plano de corte não encontrado: " + id));
    }

    private void assertDraft(CutPlan plan) {
        if (!"DRAFT".equals(plan.getStatus())) {
            throw new IllegalStateException("Operação só permitida em planos com status DRAFT. Status atual: " + plan.getStatus());
        }
    }

    private void recalculateArea(CutPlanItem item) {
        if (item.getFinalWidth() != null && item.getFinalHeight() != null) {
            BigDecimal area = item.getFinalWidth()
                    .multiply(item.getFinalHeight())
                    .multiply(new BigDecimal(item.getQuantity()));
            item.setCalculatedArea(area);
        }
    }

    private CutPlanResponse toResponse(CutPlan plan) {
        BigDecimal totalCost = plan.getItems().stream()
                .map(i -> i.getEstimatedCost() != null ? i.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQty = plan.getItems().stream().mapToInt(CutPlanItem::getQuantity).sum();
        BigDecimal totalArea = plan.getItems().stream()
                .map(i -> i.getCalculatedArea() != null ? i.getCalculatedArea() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CutPlanResponse.builder()
                .id(plan.getId())
                .workOrderId(plan.getWorkOrder() != null ? plan.getWorkOrder().getId() : null)
                .version(plan.getVersion())
                .status(plan.getStatus())
                .description(plan.getDescription())
                .items(plan.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()))
                .totalEstimatedCost(totalCost)
                .totalQuantity(totalQty)
                .totalArea(totalArea)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .createdBy(plan.getCreatedBy() != null ? plan.getCreatedBy().getUsername() : null)
                .updatedBy(plan.getUpdatedBy() != null ? plan.getUpdatedBy().getUsername() : null)
                .build();
    }

    private CutPlanListResponse toListResponse(CutPlan plan) {
        CutPlanListResponse r = new CutPlanListResponse();
        r.setId(plan.getId());
        r.setWorkOrderId(plan.getWorkOrder() != null ? plan.getWorkOrder().getId() : null);
        r.setWorkOrderNumber(plan.getWorkOrder() != null ? plan.getWorkOrder().getNumber() : null);
        r.setStatus(plan.getStatus());
        r.setVersion(plan.getVersion());
        r.setTotalItems(plan.getItems().size());
        r.setCreatedAt(plan.getCreatedAt());
        return r;
    }

    private CutPlanItemResponse toItemResponse(CutPlanItem item) {
        CutPlanItemResponse r = new CutPlanItemResponse();
        r.setId(item.getId());
        r.setDescription(item.getDescription());
        r.setEnvironment(item.getEnvironment());
        r.setGlassType(item.getGlassType());
        r.setThickness(item.getThickness());
        r.setColor(item.getColor());
        r.setFinishing(item.getFinishing());
        r.setGrossWidth(item.getGrossWidth());
        r.setGrossHeight(item.getGrossHeight());
        r.setFinalWidth(item.getFinalWidth());
        r.setFinalHeight(item.getFinalHeight());
        r.setQuantity(item.getQuantity());
        r.setCalculatedArea(item.getCalculatedArea());
        r.setEstimatedCost(item.getEstimatedCost());
        r.setNotes(item.getNotes());
        r.setSupplierId(item.getSupplier() != null ? item.getSupplier().getId() : null);
        r.setSupplierName(item.getSupplier() != null ? item.getSupplier().getName() : null);
        return r;
    }
}
