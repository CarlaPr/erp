package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.*;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final ClientService clientService;
    private final ServiceCategoryRepository categoryRepository;
    private final ProfileRepository profileRepository;
    private final QuoteRepository quoteRepository;
    private final WorkOrderRepository workOrderRepo;

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService,
                               ServiceCategoryRepository categoryRepository, ProfileRepository profileRepository,
                               QuoteRepository quoteRepository, WorkOrderRepository workOrderRepo) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.quoteRepository = quoteRepository;
        this.workOrderRepo = workOrderRepo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        // Busca todas as ordens de serviço da mais nova para a mais antiga
        List<WorkOrder> orders = workOrderRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        // Calcula os totais globais
        for (WorkOrder wo : orders) {
            if (!"cancelled".equals(wo.getStatus())) {
                BigDecimal rev = wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO;
                BigDecimal cost = wo.getTotalCost() != null ? wo.getTotalCost() : BigDecimal.ZERO;

                totalRevenue = totalRevenue.add(rev);
                totalCost = totalCost.add(cost);
            }
        }

        BigDecimal globalProfit = totalRevenue.subtract(totalCost);
        BigDecimal averageMargin = BigDecimal.ZERO;

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            averageMargin = globalProfit.multiply(new BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        orders.forEach(wo -> {
            if (wo.getItems() != null) {
                wo.getItems().size();
            }
        });
        model.addAttribute("orders", orders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("globalProfit", globalProfit);
        model.addAttribute("averageMargin", averageMargin);
        model.addAttribute("currentPage", "work-orders");

        return "work-orders";
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> saveAjax(@RequestBody WorkOrder workOrder) {
        WorkOrder targetWo;

        if (workOrder.getId() != null) {
            targetWo = workOrderService.findById(workOrder.getId());
            if (targetWo != null) {
                // Atualiza campos
                targetWo.setTitle(workOrder.getTitle());
                targetWo.setStatus(workOrder.getStatus());
                targetWo.setDescription(workOrder.getDescription());
                targetWo.setNotes(workOrder.getNotes());
                targetWo.setInstallDate(workOrder.getInstallDate());
                targetWo.setTotalValue(workOrder.getTotalValue());
                targetWo.setClient(workOrder.getClient());

                if (targetWo.getItems() == null) {
                    targetWo.setItems(new ArrayList<>());
                } else {
                    targetWo.getItems().clear();
                }
            } else {
                targetWo = workOrder;
            }
        } else {
            targetWo = workOrder;
        }

        // Adiciona os itens novos corretamente
        if (workOrder.getItems() != null) {
            for (WorkOrderItem item : workOrder.getItems()) {
                item.setId(null); // Garante que não duplica
                item.setWorkOrder(targetWo);
                targetWo.getItems().add(item);
            }
        }

        // Vinculação com Orçamento
        if (workOrder.getQuoteId() != null) {
            Quote q = quoteRepository.findById(workOrder.getQuoteId()).orElse(null);
            if (q != null) {
                targetWo.setQuote(q);
                q.setWorkOrder(targetWo);
                quoteRepository.save(q);
            }
        }

        workOrderService.save(targetWo);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/edit-data/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getEditData(
            @PathVariable UUID id
    ) {

        WorkOrder wo = workOrderService.findById(id);

        if (wo == null) {
            return ResponseEntity.notFound().build();
        }

        Hibernate.initialize(wo.getItems());
        return ResponseEntity.ok(wo);
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody String reason) {
        WorkOrder wo = workOrderService.findById(id);
        if(wo != null) {
            wo.setStatus("cancelled");
            wo.setNotes((wo.getNotes() != null ? wo.getNotes() : "") + "\n>>> CANCELADA: " + reason);
            if(wo.getQuote() != null) {
                Quote q = wo.getQuote();
                q.setWorkOrder(null);
                quoteRepository.save(q);
                wo.setQuote(null);
            }
            workOrderService.save(wo);
        }
        return ResponseEntity.ok().build();
    }
}