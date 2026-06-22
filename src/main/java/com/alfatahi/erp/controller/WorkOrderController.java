package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.WorkOrderService;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
}