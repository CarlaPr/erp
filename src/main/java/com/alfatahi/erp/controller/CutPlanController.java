package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.CutPlanService;
import com.alfatahi.erp.service.report.PdfReportService;
import org.hibernate.Hibernate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/cut-plans")
public class CutPlanController {

    private final CutPlanService cutPlanService;
    private final WorkOrderRepository workOrderRepository;
    private final CutRuleSetRepository ruleSetRepository;
    private final SupplierRepository supplierRepository;
    private final PdfReportService pdfReportService;

    public CutPlanController(CutPlanService cutPlanService,
                              WorkOrderRepository workOrderRepository,
                              CutRuleSetRepository ruleSetRepository,
                              SupplierRepository supplierRepository,
                              PdfReportService pdfReportService) {
        this.cutPlanService = cutPlanService;
        this.workOrderRepository = workOrderRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.supplierRepository = supplierRepository;
        this.pdfReportService = pdfReportService;
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    @GetMapping("/os/{workOrderId}")
    @Transactional(readOnly = true)
    public String screen(@PathVariable UUID workOrderId, Model model) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada"));
        List<CutPlan> plans = cutPlanService.listByWorkOrder(workOrderId);
        plans.forEach(p -> { Hibernate.initialize(p.getItems()); Hibernate.initialize(p.getMaterials()); });

        model.addAttribute("workOrder", wo);
        model.addAttribute("plans", plans);
        model.addAttribute("ruleSets", ruleSetRepository.findByActiveTrueOrderByNameAsc());
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("currentPage", "work-orders");
        return "cut-plans";
    }

    @PostMapping("/os/{workOrderId}")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutPlan> create(@PathVariable UUID workOrderId,
                                           @RequestBody Map<String, String> body) {
        String title = body.get("title");
        UUID ruleSetId = body.get("ruleSetId") != null && !body.get("ruleSetId").isBlank()
                ? UUID.fromString(body.get("ruleSetId")) : null;
        CutPlan plan = cutPlanService.createForWorkOrder(workOrderId, title, ruleSetId, currentUser());
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<CutPlan> get(@PathVariable UUID id) {
        CutPlan plan = cutPlanService.findById(id);
        Hibernate.initialize(plan.getItems());
        Hibernate.initialize(plan.getMaterials());
        for (CutPlanItem it : plan.getItems()) {
            Hibernate.initialize(it.getDrillings());
            Hibernate.initialize(it.getNotches());
            Hibernate.initialize(it.getChamfers());
        }
        return ResponseEntity.ok(plan);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        cutPlanService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<CutPlan> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(cutPlanService.updateStatus(id, CutPlan.Status.valueOf(body.get("status")), currentUser()));
    }

    @PostMapping("/{id}/items")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutPlanItem> saveItem(@PathVariable UUID id, @RequestBody CutPlanItem item) {
        return ResponseEntity.ok(cutPlanService.saveItem(id, item, currentUser()));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseBody
    public ResponseEntity<?> deleteItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        cutPlanService.deleteItem(id, itemId, currentUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/materials")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutPlanMaterial> saveMaterial(@PathVariable UUID id, @RequestBody CutPlanMaterial material) {
        return ResponseEntity.ok(cutPlanService.saveMaterial(id, material, currentUser()));
    }

    @DeleteMapping("/{id}/materials/{materialId}")
    @ResponseBody
    public ResponseEntity<?> deleteMaterial(@PathVariable UUID id, @PathVariable UUID materialId) {
        cutPlanService.deleteMaterial(id, materialId, currentUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/items/{itemId}/drillings")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutPlanItemDrilling> saveDrilling(@PathVariable UUID itemId, @RequestBody CutPlanItemDrilling drilling) {
        return ResponseEntity.ok(cutPlanService.saveDrilling(itemId, drilling, currentUser()));
    }

    @DeleteMapping("/items/{itemId}/drillings/{drillingId}")
    @ResponseBody
    public ResponseEntity<?> deleteDrilling(@PathVariable UUID itemId, @PathVariable UUID drillingId) {
        cutPlanService.deleteDrilling(itemId, drillingId, currentUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/items/{itemId}/notches")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutPlanItemNotch> saveNotch(@PathVariable UUID itemId, @RequestBody CutPlanItemNotch notch) {
        return ResponseEntity.ok(cutPlanService.saveNotch(itemId, notch, currentUser()));
    }

    @DeleteMapping("/items/{itemId}/notches/{notchId}")
    @ResponseBody
    public ResponseEntity<?> deleteNotch(@PathVariable UUID itemId, @PathVariable UUID notchId) {
        cutPlanService.deleteNotch(itemId, notchId, currentUser());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/items/{itemId}/chamfers")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutPlanItemChamfer> saveChamfer(@PathVariable UUID itemId, @RequestBody CutPlanItemChamfer chamfer) {
        return ResponseEntity.ok(cutPlanService.saveChamfer(itemId, chamfer, currentUser()));
    }

    @DeleteMapping("/items/{itemId}/chamfers/{chamferId}")
    @ResponseBody
    public ResponseEntity<?> deleteChamfer(@PathVariable UUID itemId, @PathVariable UUID chamferId) {
        cutPlanService.deleteChamfer(itemId, chamferId, currentUser());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/items/{itemId}/drawing", produces = "image/svg+xml")
    @ResponseBody
    public ResponseEntity<String> drawing(@PathVariable UUID itemId) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(cutPlanService.renderDrawing(itemId));
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public ResponseEntity<List<CutPlanHistory>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(cutPlanService.history(id));
    }

    @GetMapping("/{id}/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        CutPlan plan = cutPlanService.findById(id);
        Hibernate.initialize(plan.getItems());
        Hibernate.initialize(plan.getMaterials());

        Map<UUID, String> itemDrawings = new java.util.LinkedHashMap<>();
        for (CutPlanItem it : plan.getItems()) {
            Hibernate.initialize(it.getDrillings());
            Hibernate.initialize(it.getNotches());
            Hibernate.initialize(it.getChamfers());
            itemDrawings.put(it.getId(), cutPlanService.renderDrawing(it.getId()));
        }

        Context ctx = new Context();
        ctx.setVariable("plan", plan);
        ctx.setVariable("workOrder", plan.getWorkOrder());
        ctx.setVariable("itemDrawings", itemDrawings);
        ctx.setVariable("generatedAt", java.time.LocalDateTime.now());

        byte[] pdf = pdfReportService.render("cut-plan-pdf", ctx);

        String fileName = "pedido-tempera-" + plan.getWorkOrder().getNumber() + "-" + plan.getPlanNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
