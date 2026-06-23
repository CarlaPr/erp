package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.WorkOrderService;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final ClientService clientService;
    private final ServiceCategoryRepository categoryRepository;

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService, ServiceCategoryRepository categoryRepository) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public String index(Model model) {
        List<WorkOrder> workOrders = workOrderService.listAll();

        // Cálculo dos 4 Cards de Topo
        BigDecimal receitaTotal = workOrders.stream().map(wo -> wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal custoTotal = workOrders.stream().map(WorkOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lucroBruto = receitaTotal.subtract(custoTotal);
        BigDecimal margemMedia = receitaTotal.compareTo(BigDecimal.ZERO) > 0
                ? lucroBruto.multiply(new BigDecimal("100")).divide(receitaTotal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        model.addAttribute("currentPage", "work-orders");
        model.addAttribute("workOrders", workOrders);
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("categories", categoryRepository.findAll());

        model.addAttribute("receitaTotal", receitaTotal);
        model.addAttribute("custoTotal", custoTotal);
        model.addAttribute("lucroBruto", lucroBruto);
        model.addAttribute("margemMedia", margemMedia);

        return "work-orders";
    }

    @GetMapping("/edit-data/{id}")
    @ResponseBody
    public ResponseEntity<WorkOrder> getWorkOrderData(@PathVariable UUID id) {
        // Envia os dados completos da O.S. (incluindo a lista de custos) para popular o modal via JS
        return ResponseEntity.ok(workOrderService.findById(id));
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> saveAjax(@RequestBody WorkOrder workOrder) {
        // Garante que todos os itens recebem o ID da O.S. antes de guardar em cascata
        if (workOrder.getItems() != null) {
            for (WorkOrderItem item : workOrder.getItems()) {
                item.setWorkOrder(workOrder);
            }
        }
        workOrderService.save(workOrder);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") UUID id) {
        workOrderService.delete(id);
        return "redirect:/work-orders";
    }
}