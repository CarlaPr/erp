package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.WorkOrderService;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import com.alfatahi.erp.repository.WorkOrderItemRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final ClientService clientService;
    private final ServiceCategoryRepository categoryRepository;
    private final WorkOrderItemRepository workOrderItemRepository;

    public WorkOrderController(WorkOrderService workOrderService,
                               ClientService clientService,
                               ServiceCategoryRepository categoryRepository,
                               WorkOrderItemRepository workOrderItemRepository) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.workOrderItemRepository = workOrderItemRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("currentPage", "work-orders");
        model.addAttribute("workOrders", workOrderService.listAll());
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("newWorkOrder", new WorkOrder());
        return "work-orders";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newWorkOrder") WorkOrder workOrder) {
        workOrderService.save(workOrder);
        return "redirect:/work-orders";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") UUID id, Model model) {
        model.addAttribute("currentPage", "work-orders");
        model.addAttribute("workOrders", workOrderService.listAll());
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("newWorkOrder", workOrderService.findById(id));
        model.addAttribute("isEditing", true);
        return "work-orders";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") UUID id) {
        workOrderService.delete(id);
        return "redirect:/work-orders";
    }

    // ==================================================
    // NOVAS ROTAS AUDITADAS: GESTÃO GRANULAR DE MATERIAIS
    // ==================================================

    @GetMapping("/{id}/items")
    public String viewItems(@PathVariable("id") UUID id, Model model) {
        WorkOrder wo = workOrderService.findById(id);
        model.addAttribute("currentPage", "work-orders");
        model.addAttribute("workOrder", wo);
        model.addAttribute("items", workOrderItemRepository.findByWorkOrderId(id));
        model.addAttribute("newItem", new WorkOrderItem());
        model.addAttribute("totalCost", workOrderService.calculateObraCost(id));
        return "work-order-items";
    }

    @PostMapping("/{id}/items/save")
    public String saveItem(@PathVariable("id") UUID id, @ModelAttribute("newItem") WorkOrderItem item) {
        WorkOrder wo = workOrderService.findById(id);
        item.setWorkOrder(wo);
        workOrderItemRepository.save(item);

        BigDecimal newTotal = workOrderItemRepository.findByWorkOrderId(id).stream()
                .map(WorkOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wo.setTotalValue(newTotal);
        workOrderService.save(wo);

        return "redirect:/work-orders/" + id + "/items";
    }

    @GetMapping("/{woId}/items/delete/{itemId}")
    public String deleteItem(@PathVariable("woId") UUID woId, @PathVariable("itemId") UUID itemId) {
        workOrderItemRepository.deleteById(itemId);

        WorkOrder wo = workOrderService.findById(woId);
        BigDecimal newTotal = workOrderItemRepository.findByWorkOrderId(woId).stream()
                .map(WorkOrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        wo.setTotalValue(newTotal);
        workOrderService.save(wo);

        return "redirect:/work-orders/" + woId + "/items";
    }
}