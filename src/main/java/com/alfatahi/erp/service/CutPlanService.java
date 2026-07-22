package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class CutPlanService {

    private final CutPlanRepository cutPlanRepository;
    private final CutPlanItemRepository itemRepository;
    private final CutPlanMaterialRepository materialRepository;
    private final CutPlanHistoryRepository historyRepository;
    private final CutRuleSetRepository ruleSetRepository;
    private final WorkOrderRepository workOrderRepository;
    private final MaterialPriceService materialPriceService;
    private final CutPlanItemDrillingRepository drillingRepository;
    private final CutPlanItemNotchRepository notchRepository;
    private final CutPlanItemChamferRepository chamferRepository;
    private final GlassDrawingService glassDrawingService;

    public CutPlanService(CutPlanRepository cutPlanRepository, CutPlanItemRepository itemRepository,
                           CutPlanMaterialRepository materialRepository, CutPlanHistoryRepository historyRepository,
                           CutRuleSetRepository ruleSetRepository, WorkOrderRepository workOrderRepository,
                           MaterialPriceService materialPriceService,
                           CutPlanItemDrillingRepository drillingRepository, CutPlanItemNotchRepository notchRepository,
                           CutPlanItemChamferRepository chamferRepository, GlassDrawingService glassDrawingService) {
        this.cutPlanRepository = cutPlanRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.historyRepository = historyRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.workOrderRepository = workOrderRepository;
        this.materialPriceService = materialPriceService;
        this.drillingRepository = drillingRepository;
        this.notchRepository = notchRepository;
        this.chamferRepository = chamferRepository;
        this.glassDrawingService = glassDrawingService;
    }

    public List<CutPlan> listByWorkOrder(UUID workOrderId) {
        return cutPlanRepository.findByWorkOrderIdOrderByPlanNumberAsc(workOrderId);
    }

    public CutPlan findById(UUID id) {
        return cutPlanRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Plano de Corte não encontrado"));
    }

    @Transactional
    public CutPlan createForWorkOrder(UUID workOrderId, String title, UUID ruleSetId, String userLogin) {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de Serviço não encontrada"));

        long existing = cutPlanRepository.countByWorkOrderId(workOrderId);

        CutPlan plan = new CutPlan();
        plan.setWorkOrder(wo);
        plan.setPlanNumber((int) existing + 1);
        plan.setTitle(title != null && !title.isBlank() ? title
                : "Plano de Corte " + String.format("%02d", existing + 1));
        plan.setStatus(CutPlan.Status.draft);
        plan.setOrigin(wo.getQuote() != null ? "quote" : "manual");
        plan.setResponsible(userLogin);
        plan.setCreatedBy(userLogin);

        if (ruleSetId != null) {
            ruleSetRepository.findById(ruleSetId).ifPresent(plan::setRuleSet);
        }

        CutPlan saved = cutPlanRepository.save(plan);
        logHistory(saved, "created", "Plano de corte criado para a OS " + wo.getNumber(), userLogin);
        return saved;
    }

    @Transactional
    public CutPlanItem saveItem(UUID cutPlanId, CutPlanItem incoming, String userLogin) {
        CutPlan plan = findById(cutPlanId);

        CutPlanItem target = incoming.getId() != null
                ? itemRepository.findById(incoming.getId()).orElse(new CutPlanItem())
                : new CutPlanItem();

        target.setCutPlan(plan);
        target.setDescription(incoming.getDescription());
        target.setEnvironment(incoming.getEnvironment());
        target.setGlassType(incoming.getGlassType());
        target.setThickness(incoming.getThickness());
        target.setColor(incoming.getColor());
        target.setFinish(incoming.getFinish());
        target.setGrossWidth(incoming.getGrossWidth());
        target.setGrossHeight(incoming.getGrossHeight());
        target.setQuantity(incoming.getQuantity());
        target.setAngleType(incoming.getAngleType());
        target.setEdgeWork(incoming.getEdgeWork());
        target.setDrillingCount(incoming.getDrillingCount());
        target.setNotchCount(incoming.getNotchCount());
        target.setSupplier(incoming.getSupplier());
        target.setObservations(incoming.getObservations());
        target.setSortOrder(incoming.getSortOrder());

        applyDiscountRules(target, plan.getRuleSet());

        materialPriceService.findCurrentGlassPrice(target.getGlassType(), target.getColor(), target.getFinish(), target.getThickness())
                .ifPresentOrElse(price -> {
                    target.setPriceItem(price);
                    target.setUnitPriceSnapshot(price.getPrice());
                }, () -> {
                    if (target.getPriceItem() == null) target.setUnitPriceSnapshot(BigDecimal.ZERO);
                });

        boolean isNew = target.getId() == null;
        CutPlanItem saved = itemRepository.save(target);
        plan.setUpdatedAt(LocalDateTime.now());
        cutPlanRepository.save(plan);

        logHistory(plan, isNew ? "item_added" : "item_updated",
                (isNew ? "Peça adicionada: " : "Peça atualizada: ") + saved.getDescription(), userLogin);
        return saved;
    }

    @Transactional
    public void deleteItem(UUID cutPlanId, UUID itemId, String userLogin) {
        CutPlan plan = findById(cutPlanId);
        itemRepository.deleteById(itemId);
        logHistory(plan, "item_removed", "Peça removida (id " + itemId + ")", userLogin);
    }

    public void applyDiscountRules(CutPlanItem item, CutRuleSet ruleSet) {
        if (item.getGrossWidth() == null || item.getGrossHeight() == null) {
            return;
        }
        BigDecimal widthDiscount = BigDecimal.ZERO;
        BigDecimal heightDiscount = BigDecimal.ZERO;

        if (ruleSet != null && ruleSet.getParameters() != null) {
            for (CutRuleParameter p : ruleSet.getParameters()) {
                if (p.getDimension() == CutRuleParameter.Dimension.WIDTH || p.getDimension() == CutRuleParameter.Dimension.BOTH) {
                    widthDiscount = widthDiscount.add(p.getValueMm());
                }
                if (p.getDimension() == CutRuleParameter.Dimension.HEIGHT || p.getDimension() == CutRuleParameter.Dimension.BOTH) {
                    heightDiscount = heightDiscount.add(p.getValueMm());
                }
            }
        }

        BigDecimal finalW = item.getGrossWidth().subtract(widthDiscount);
        BigDecimal finalH = item.getGrossHeight().subtract(heightDiscount);
        item.setFinalWidth(finalW.max(BigDecimal.ZERO));
        item.setFinalHeight(finalH.max(BigDecimal.ZERO));
    }

    @Transactional
    public CutPlanMaterial saveMaterial(UUID cutPlanId, CutPlanMaterial incoming, String userLogin) {
        CutPlan plan = findById(cutPlanId);
        CutPlanMaterial target = incoming.getId() != null
                ? materialRepository.findById(incoming.getId()).orElse(new CutPlanMaterial())
                : new CutPlanMaterial();

        target.setCutPlan(plan);
        target.setCategory(incoming.getCategory());
        target.setDescription(incoming.getDescription());
        target.setQuantity(incoming.getQuantity());
        target.setUnit(incoming.getUnit());
        target.setSupplierName(incoming.getSupplierName());
        target.setNotes(incoming.getNotes());

        if (incoming.getPriceItem() != null && incoming.getPriceItem().getId() != null) {
            target.setPriceItem(incoming.getPriceItem());
            target.setUnitPriceSnapshot(incoming.getUnitPriceSnapshot());
        } else {
            target.setUnitPriceSnapshot(incoming.getUnitPriceSnapshot());
        }

        boolean isNew = target.getId() == null;
        CutPlanMaterial saved = materialRepository.save(target);
        logHistory(plan, isNew ? "material_added" : "material_updated", saved.getDescription(), userLogin);
        return saved;
    }

    @Transactional
    public void deleteMaterial(UUID cutPlanId, UUID materialId, String userLogin) {
        CutPlan plan = findById(cutPlanId);
        materialRepository.deleteById(materialId);
        logHistory(plan, "material_removed", "Material removido (id " + materialId + ")", userLogin);
    }

    @Transactional
    public CutPlan updateStatus(UUID cutPlanId, CutPlan.Status status, String userLogin) {
        CutPlan plan = findById(cutPlanId);
        plan.setStatus(status);
        plan.setUpdatedAt(LocalDateTime.now());
        CutPlan saved = cutPlanRepository.save(plan);
        logHistory(saved, "status_changed", "Status alterado para " + status, userLogin);
        return saved;
    }

    @Transactional
    public void delete(UUID cutPlanId) {
        cutPlanRepository.deleteById(cutPlanId);
    }

    public List<CutPlanHistory> history(UUID cutPlanId) {
        return historyRepository.findByCutPlanIdOrderByChangedAtDesc(cutPlanId);
    }

    private void logHistory(CutPlan plan, String action, String details, String userLogin) {
        CutPlanHistory h = new CutPlanHistory();
        h.setCutPlan(plan);
        h.setAction(action);
        h.setDetails(details);
        h.setChangedBy(userLogin);
        historyRepository.save(h);
    }

    public BigDecimal getTotalEstimatedCostForWorkOrder(UUID workOrderId) {
        return listByWorkOrder(workOrderId).stream()
                .filter(p -> p.getStatus() != CutPlan.Status.cancelled)
                .map(CutPlan::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CutPlanItem findItem(UUID itemId) {
        return itemRepository.findById(itemId).orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
    }

    @Transactional
    public CutPlanItemDrilling saveDrilling(UUID itemId, CutPlanItemDrilling incoming, String userLogin) {
        CutPlanItem item = findItem(itemId);
        CutPlanItemDrilling target = incoming.getId() != null
                ? drillingRepository.findById(incoming.getId()).orElse(new CutPlanItemDrilling())
                : new CutPlanItemDrilling();
        target.setCutPlanItem(item);
        target.setPosX(incoming.getPosX());
        target.setPosY(incoming.getPosY());
        target.setDiameter(incoming.getDiameter());
        target.setDrillType(incoming.getDrillType());
        target.setNotes(incoming.getNotes());
        CutPlanItemDrilling saved = drillingRepository.save(target);
        logHistory(item.getCutPlan(), "drilling_saved", "Furo em " + item.getDescription(), userLogin);
        return saved;
    }

    @Transactional
    public void deleteDrilling(UUID itemId, UUID drillingId, String userLogin) {
        CutPlanItem item = findItem(itemId);
        drillingRepository.deleteById(drillingId);
        logHistory(item.getCutPlan(), "drilling_removed", "Furo removido em " + item.getDescription(), userLogin);
    }

    @Transactional
    public CutPlanItemNotch saveNotch(UUID itemId, CutPlanItemNotch incoming, String userLogin) {
        CutPlanItem item = findItem(itemId);
        CutPlanItemNotch target = incoming.getId() != null
                ? notchRepository.findById(incoming.getId()).orElse(new CutPlanItemNotch())
                : new CutPlanItemNotch();
        target.setCutPlanItem(item);
        target.setPosX(incoming.getPosX());
        target.setPosY(incoming.getPosY());
        target.setWidth(incoming.getWidth());
        target.setHeight(incoming.getHeight());
        target.setNotes(incoming.getNotes());
        CutPlanItemNotch saved = notchRepository.save(target);
        logHistory(item.getCutPlan(), "notch_saved", "Recorte em " + item.getDescription(), userLogin);
        return saved;
    }

    @Transactional
    public void deleteNotch(UUID itemId, UUID notchId, String userLogin) {
        CutPlanItem item = findItem(itemId);
        notchRepository.deleteById(notchId);
        logHistory(item.getCutPlan(), "notch_removed", "Recorte removido em " + item.getDescription(), userLogin);
    }

    @Transactional
    public CutPlanItemChamfer saveChamfer(UUID itemId, CutPlanItemChamfer incoming, String userLogin) {
        CutPlanItem item = findItem(itemId);
        CutPlanItemChamfer target = incoming.getId() != null
                ? chamferRepository.findById(incoming.getId()).orElse(new CutPlanItemChamfer())
                : new CutPlanItemChamfer();
        target.setCutPlanItem(item);
        target.setCorner(incoming.getCorner());
        target.setSize(incoming.getSize());
        target.setNotes(incoming.getNotes());
        CutPlanItemChamfer saved = chamferRepository.save(target);
        logHistory(item.getCutPlan(), "chamfer_saved", "Chanfro em " + item.getDescription(), userLogin);
        return saved;
    }

    @Transactional
    public void deleteChamfer(UUID itemId, UUID chamferId, String userLogin) {
        CutPlanItem item = findItem(itemId);
        chamferRepository.deleteById(chamferId);
        logHistory(item.getCutPlan(), "chamfer_removed", "Chanfro removido em " + item.getDescription(), userLogin);
    }

    @Transactional(readOnly = true)
    public String renderDrawing(UUID itemId) {
        CutPlanItem item = findItem(itemId);
        List<CutPlanItem> siblings = itemRepository.findByCutPlanIdOrderBySortOrderAsc(item.getCutPlan().getId());
        int pieceNumber = Math.max(1, siblings.indexOf(item) + 1);
        return glassDrawingService.render(item, pieceNumber);
    }
}
