package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService,
                               ServiceCategoryRepository categoryRepository, ProfileRepository profileRepository,
                               QuoteRepository quoteRepository) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.quoteRepository = quoteRepository;
    }

    @GetMapping
    public String index(Model model) {
        List<WorkOrder> allOrders = workOrderService.listAll();

        // Filtra ativas para KPIs
        List<WorkOrder> active = allOrders.stream().filter(wo -> !"cancelled".equals(wo.getStatus()) && !"canceled".equals(wo.getStatus())).collect(Collectors.toList());

        BigDecimal receita = active.stream().map(WorkOrder::getTotalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal custo = active.stream().map(WorkOrder::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lucro = receita.subtract(custo);
        BigDecimal margem = receita.compareTo(BigDecimal.ZERO) > 0 ? lucro.multiply(new BigDecimal("100")).divide(receita, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        model.addAttribute("workOrders", allOrders);
        model.addAttribute("availableQuotes", quoteRepository.findAll().stream().filter(q -> "approved".equals(q.getStatus())).collect(Collectors.toList()));
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("profile", profileRepository.findAll().stream().findFirst().orElse(null));
        model.addAttribute("receitaTotal", receita);
        model.addAttribute("custoTotal", custo);
        model.addAttribute("lucroBruto", lucro);
        model.addAttribute("margemMedia", margem);
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

                // Limpa a lista atual. Graças ao orphanRemoval=true na Entidade,
                // o Hibernate apaga os itens antigos sozinho.
                targetWo.getItems().clear();
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