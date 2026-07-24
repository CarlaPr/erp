package com.alfatahi.erp.cutplan.controller;

import com.alfatahi.erp.cutplan.dto.*;
import com.alfatahi.erp.cutplan.entity.CutPlan;
import com.alfatahi.erp.cutplan.entity.CutPlanItem;
import com.alfatahi.erp.cutplan.repository.CutPlanRepository;
import com.alfatahi.erp.cutplan.service.CutPlanService;
import com.alfatahi.erp.cutplan.service.TechnicalRulesService;
import com.alfatahi.erp.entity.ServiceCategory;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.cutplan.entity.GlassCutRule;
import com.alfatahi.erp.cutplan.repository.GlassCutRuleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/cut-plans")
public class CutPlanWebController {

    @Autowired
    private CutPlanRepository cutPlanRepository;

    @Autowired
    private CutPlanService cutPlanService;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ServiceCategoryRepository serviceCategoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private GlassCutRuleRepository glassCutRuleRepository;

    @Autowired
    private TechnicalRulesService technicalRulesService;

    // ──────────────────────────────────────────────────
    // LISTAGEM PRINCIPAL
    // ──────────────────────────────────────────────────

    @GetMapping
    public String listCutPlans(Model model,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String search) {
        List<CutPlan> plans;
        if (status != null && !status.isBlank()) {
            plans = cutPlanRepository.findByStatus(status);
        } else {
            plans = cutPlanRepository.findAll();
        }

        // Search filter
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            plans = plans.stream()
                    .filter(p -> {
                        if (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)) return true;
                        if (p.getWorkOrder() != null) {
                            WorkOrder wo = p.getWorkOrder();
                            if (wo.getNumber() != null && wo.getNumber().toLowerCase().contains(q)) return true;
                            if (wo.getTitle() != null && wo.getTitle().toLowerCase().contains(q)) return true;
                            if (wo.getClient() != null && wo.getClient().getName() != null
                                    && wo.getClient().getName().toLowerCase().contains(q)) return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        plans.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        // Stats
        long draft = cutPlanRepository.countByStatus("DRAFT");
        long approved = cutPlanRepository.countByStatus("APPROVED");
        long sent = cutPlanRepository.countByStatus("SENT_TO_SUPPLIER");
        long cancelled = cutPlanRepository.countByStatus("CANCELLED");

        model.addAttribute("plans", plans);
        model.addAttribute("statusFilter", status);
        model.addAttribute("search", search);
        model.addAttribute("countDraft", draft);
        model.addAttribute("countApproved", approved);
        model.addAttribute("countSent", sent);
        model.addAttribute("countCancelled", cancelled);
        model.addAttribute("currentPage", "cut-plans");

        // Work orders without cut plan for quick creation
        List<WorkOrder> workOrders = workOrderRepository.findAll().stream()
                .filter(wo -> !cutPlanRepository.existsByWorkOrderId(wo.getId()))
                .filter(wo -> !"concluida".equalsIgnoreCase(wo.getStatus()) && !"cancelada".equalsIgnoreCase(wo.getStatus()))
                .sorted(Comparator.comparing(WorkOrder::getNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        model.addAttribute("workOrders", workOrders);

        return "cut-plans";
    }

    // ──────────────────────────────────────────────────
    // DETALHE / EDIÇÃO
    // ──────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String viewCutPlan(@PathVariable UUID id, Model model) {
        CutPlan plan = cutPlanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado: " + id));

        List<Supplier> suppliers = supplierRepository.findAll()
                .stream().sorted(Comparator.comparing(Supplier::getName)).collect(Collectors.toList());

        // Summary stats
        BigDecimal totalAreaM2 = plan.getItems().stream()
                .map(i -> i.getCalculatedArea() != null ? i.getCalculatedArea() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("1000000"), 4, RoundingMode.HALF_UP);

        int totalQty = plan.getItems().stream().mapToInt(CutPlanItem::getQuantity).sum();

        model.addAttribute("plan", plan);
        model.addAttribute("workOrder", plan.getWorkOrder());
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("totalAreaM2", totalAreaM2);
        model.addAttribute("totalQty", totalQty);
        model.addAttribute("currentPage", "cut-plans");
        return "cut-plan-detail";
    }

    // ──────────────────────────────────────────────────
    // ADICIONAR ITEM (AJAX)
    // ──────────────────────────────────────────────────

    @PostMapping("/{id}/items")
    @ResponseBody
    public ResponseEntity<?> addItem(@PathVariable UUID id,
                                      @RequestBody CutPlanItemRequest request,
                                      @AuthenticationPrincipal User user) {
        try {
            String userId = user != null ? user.getUsername() : null;
            // Apply technical rules if category available
            CutPlan plan = cutPlanRepository.findById(id).orElseThrow();
            if (plan.getWorkOrder() != null && plan.getWorkOrder().getCategory() != null) {
                // Create temp item to apply rules
                ServiceCategory cat = plan.getWorkOrder().getCategory();
                CutPlanItem temp = new CutPlanItem();
                temp.setGrossWidth(request.getGrossWidth());
                temp.setGrossHeight(request.getGrossHeight());
                temp.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
                technicalRulesService.applyRules(temp, cat);
                if (request.getFinalWidth() == null) request.setFinalWidth(temp.getFinalWidth());
                if (request.getFinalHeight() == null) request.setFinalHeight(temp.getFinalHeight());
            }

            CutPlanResponse resp = cutPlanService.addItem(id, request,
                    userId != null ? UUID.fromString(userId) : UUID.randomUUID());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Erro ao adicionar item: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseBody
    public ResponseEntity<?> removeItem(@PathVariable UUID id,
                                         @PathVariable UUID itemId,
                                         @AuthenticationPrincipal User user) {
        try {
            String userId = user != null ? user.getUsername() : null;
            CutPlanResponse resp = cutPlanService.removeItem(id, itemId,
                    userId != null ? UUID.fromString(userId) : UUID.randomUUID());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────
    // AÇÕES DE STATUS
    // ──────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            String userId = user != null ? user.getUsername() : null;
            cutPlanService.approvePlan(id, userId != null ? UUID.fromString(userId) : UUID.randomUUID());
        } catch (Exception e) {
            log.error("Erro ao aprovar plano: {}", e.getMessage());
        }
        return "redirect:/cut-plans/" + id + "?approved";
    }

    @PostMapping("/{id}/send-to-supplier")
    public String sendToSupplier(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            String userId = user != null ? user.getUsername() : null;
            cutPlanService.sendToSupplier(id, userId != null ? UUID.fromString(userId) : UUID.randomUUID());
        } catch (Exception e) {
            log.error("Erro ao enviar para fornecedor: {}", e.getMessage());
        }
        return "redirect:/cut-plans/" + id + "?sent";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable UUID id,
                          @RequestParam(defaultValue = "Cancelado pelo usuário") String reason,
                          @AuthenticationPrincipal User user) {
        try {
            String userId = user != null ? user.getUsername() : null;
            cutPlanService.cancelPlan(id, reason, userId != null ? UUID.fromString(userId) : UUID.randomUUID());
        } catch (Exception e) {
            log.error("Erro ao cancelar plano: {}", e.getMessage());
        }
        return "redirect:/cut-plans/" + id + "?cancelled";
    }

    @PostMapping("/{id}/reopen")
    public String reopen(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        try {
            CutPlan plan = cutPlanRepository.findById(id).orElseThrow();
            plan.setStatus("DRAFT");
            plan.setUpdatedAt(LocalDateTime.now());
            cutPlanRepository.save(plan);
        } catch (Exception e) {
            log.error("Erro ao reabrir plano: {}", e.getMessage());
        }
        return "redirect:/cut-plans/" + id + "?reopened";
    }

    // ──────────────────────────────────────────────────
    // CRIAR PLANO DE UMA OS
    // ──────────────────────────────────────────────────

    @PostMapping("/from-work-order/{woId}")
    public String createFromWorkOrder(@PathVariable UUID woId,
                                       @AuthenticationPrincipal User user) {
        try {
            String userId = user != null ? user.getUsername() : null;
            CutPlanResponse resp = cutPlanService.createFromWorkOrder(woId,
                    userId != null ? UUID.fromString(userId) : UUID.randomUUID());
            return "redirect:/cut-plans/" + resp.getId() + "?created";
        } catch (Exception e) {
            log.error("Erro ao criar plano: {}", e.getMessage());
            return "redirect:/cut-plans?error=" + e.getMessage();
        }
    }

    // ──────────────────────────────────────────────────
    // DOWNLOAD PDF - PEDIDO PARA TÊMPERA
    // ──────────────────────────────────────────────────

    @GetMapping("/{id}/pdf/tempera")
    public String pdfTempera(@PathVariable UUID id, Model model) {
        CutPlan plan = cutPlanRepository.findById(id).orElseThrow();
        preparePdfModel(plan, model);
        model.addAttribute("printType", "TEMPERA");
        return "cut-plan-pdf-tempera";
    }

    @GetMapping("/{id}/pdf/material")
    public String pdfMaterial(@PathVariable UUID id, Model model) {
        CutPlan plan = cutPlanRepository.findById(id).orElseThrow();
        preparePdfModel(plan, model);
        model.addAttribute("printType", "MATERIAL");
        return "cut-plan-pdf-material";
    }

    private void preparePdfModel(CutPlan plan, Model model) {
        BigDecimal totalAreaM2 = plan.getItems().stream()
                .map(i -> i.getCalculatedArea() != null ? i.getCalculatedArea() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal("1000000"), 4, RoundingMode.HALF_UP);

        int totalQty = plan.getItems().stream().mapToInt(CutPlanItem::getQuantity).sum();

        model.addAttribute("plan", plan);
        model.addAttribute("workOrder", plan.getWorkOrder());
        model.addAttribute("totalAreaM2", totalAreaM2);
        model.addAttribute("totalQty", totalQty);
    }

    // ──────────────────────────────────────────────────
    // API: REGRAS POR CATEGORIA
    // ──────────────────────────────────────────────────

    @GetMapping("/api/rules/{categoryId}")
    @ResponseBody
    public ResponseEntity<?> getRulesByCategory(@PathVariable UUID categoryId) {
        List<GlassCutRule> rules = glassCutRuleRepository.findByServiceCategoryId(categoryId);
        return ResponseEntity.ok(rules.stream().map(r -> Map.of(
                "parameterName", r.getParameterName(),
                "value", r.getValue(),
                "unit", r.getUnit(),
                "description", r.getDescription() != null ? r.getDescription() : "",
                "ruleType", r.getRuleType()
        )).collect(Collectors.toList()));
    }
}
