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
    private final ClientRepository clientRepository;
    private final AccountsReceivableRepository receivableRepo;

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService,
                               ServiceCategoryRepository categoryRepository, ProfileRepository profileRepository,
                               QuoteRepository quoteRepository, WorkOrderRepository workOrderRepo,
                               ClientRepository clientRepository, AccountsReceivableRepository receivableRepo) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.quoteRepository = quoteRepository;
        this.workOrderRepo = workOrderRepo;
        this.clientRepository = clientRepository;
        this.receivableRepo = receivableRepo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        List<WorkOrder> orders = workOrderRepo.findAllWithItemsOrderByCreatedAtDesc();

        com.alfatahi.erp.entity.Profile profile = profileRepository.findAll().stream().findFirst().orElseGet(() -> {
            com.alfatahi.erp.entity.Profile p = new com.alfatahi.erp.entity.Profile();
            p.setCompanyName("Alfa Tahi");
            return profileRepository.save(p);
        });

        BigDecimal totalRevenue = workOrderRepo.sumTotalRevenue();
        BigDecimal totalCost = workOrderRepo.sumTotalCost();

        totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        totalCost = totalCost != null ? totalCost : BigDecimal.ZERO;

        BigDecimal globalProfit = totalRevenue.subtract(totalCost);
        BigDecimal averageMargin = BigDecimal.ZERO;

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            averageMargin = globalProfit.multiply(new BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP);
        }

        model.addAttribute("orders", orders);

        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("globalProfit", globalProfit);
        model.addAttribute("averageMargin", averageMargin);

        model.addAttribute("profile", profile);
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("availableQuotes", quoteRepository.findAll());
        model.addAttribute("currentPage", "work-orders");

        return "work-orders";
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> saveAjax(@RequestBody WorkOrder workOrder) {
        WorkOrder targetWo;

        if (workOrder.getId() != null) {
            targetWo = workOrderService.findById(workOrder.getId());
            if (targetWo == null) return ResponseEntity.notFound().build();

            targetWo.setTitle(workOrder.getTitle());
            targetWo.setStatus(workOrder.getStatus());
            targetWo.setDescription(workOrder.getDescription());
            targetWo.setNotes(workOrder.getNotes());
            targetWo.setInstallDate(workOrder.getInstallDate());

            if (workOrder.getTotalValue() != null && workOrder.getTotalValue().compareTo(BigDecimal.ZERO) > 0) {
                targetWo.setTotalValue(workOrder.getTotalValue());
            } else {
                targetWo.setTotalValue(workOrderService.calculateTotalValueFromItems(workOrder));
            }

            targetWo.setClient(workOrder.getClient());

            targetWo.getItems().clear();
        } else {
            targetWo = workOrder;
        }

        if (workOrder.getItems() != null) {
            for (WorkOrderItem item : workOrder.getItems()) {
                item.setWorkOrder(targetWo);
                targetWo.getItems().add(item);
            }
        }

        if (workOrder.getQuoteId() != null) {
            Quote q = quoteRepository.findById(workOrder.getQuoteId()).orElse(null);
            if (q != null) {
                targetWo.setQuote(q);
                q.setWorkOrder(targetWo);
            }
        }

        workOrderService.save(targetWo);

        if (targetWo.getInstallDate() != null && targetWo.getId() != null) {
            List<AccountsReceivable> receivables = receivableRepo.findAll().stream()
                    .filter(r -> r.getWorkOrder() != null && r.getWorkOrder().getId().equals(targetWo.getId()))
                    .collect(Collectors.toList());

            for (AccountsReceivable ar : receivables) {
                ar.setDueDate(targetWo.getInstallDate());
                receivableRepo.save(ar);
            }
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/new")
    @ResponseBody
    public ResponseEntity<WorkOrder> newOs() {
        WorkOrder wo = new WorkOrder();
        wo.setStatus("pending");
        wo.setCreatedAt(java.time.LocalDateTime.now());

        return ResponseEntity.ok(wo);
    }

    @GetMapping("/edit-data/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<?> getEditData(@PathVariable UUID id) {

        WorkOrder wo = workOrderService.findById(id);

        if (wo == null) {
            return ResponseEntity.notFound().build();
        }

        Hibernate.initialize(wo.getItems());
        return ResponseEntity.ok(wo);
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody String reason) {WorkOrder wo = workOrderService.findById(id);
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

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        workOrderService.delete(id);
        return ResponseEntity.ok().build();
    }

}