package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.*;
import org.hibernate.Hibernate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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
    private final WorkOrderRepository workOrderRepo;
    private final ClientRepository clientRepository;
    private final AccountsReceivableRepository receivableRepo;
    private final AccountsPayableRepository payableRepo;

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService,
                               ServiceCategoryRepository categoryRepository, ProfileRepository profileRepository,
                               QuoteRepository quoteRepository, WorkOrderRepository workOrderRepo,
                               ClientRepository clientRepository, AccountsReceivableRepository receivableRepo,
                               AccountsPayableRepository payableRepo) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.quoteRepository = quoteRepository;
        this.workOrderRepo = workOrderRepo;
        this.clientRepository = clientRepository;
        this.receivableRepo = receivableRepo;
        this.payableRepo = payableRepo;
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

        BigDecimal totalRevenue = orders.stream()
                .filter(wo -> !"cancelled".equals(wo.getStatus()) && !"canceled".equals(wo.getStatus()))
                .map(wo -> wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = orders.stream()
                .filter(wo -> !"cancelled".equals(wo.getStatus()) && !"canceled".equals(wo.getStatus()))
                .map(wo -> wo.getTotalCost() != null ? wo.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        boolean isTotalChanged = false;
        String changeReasonMsg = "Valor ajustado conforme alteração na O.S.";

        if (workOrder.getId() != null) {
            targetWo = workOrderService.findById(workOrder.getId());
            if (targetWo == null) return ResponseEntity.notFound().build();

            BigDecimal oldTotal = targetWo.getTotalValue();

            targetWo.setTitle(workOrder.getTitle());
            targetWo.setStatus(workOrder.getStatus());
            targetWo.setDescription(workOrder.getDescription());

            targetWo.setNotes(workOrder.getNotes());
            targetWo.setInstallDate(workOrder.getInstallDate());

            BigDecimal newTotal = workOrder.getTotalValue();
            if (newTotal != null && newTotal.compareTo(BigDecimal.ZERO) > 0) {
                targetWo.setTotalValue(newTotal);
            } else {
                targetWo.setTotalValue(workOrderService.calculateTotalValueFromItems(workOrder));
            }

            if (oldTotal != null && targetWo.getTotalValue().compareTo(oldTotal) != 0) {
                isTotalChanged = true;

                String osNotes = workOrder.getNotes() != null ? workOrder.getNotes() : "";
                if (osNotes.contains("VALOR ALTERADO")) {
                    String[] lines = osNotes.split("\n");
                    for (int i = lines.length - 1; i >= 0; i--) {
                        if (lines[i].contains("VALOR ALTERADO")) {
                            changeReasonMsg = lines[i];
                            break;
                        }
                    }
                }
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

            if (!receivables.isEmpty()) {
                int installmentsCount = receivables.size();
                BigDecimal newTotal = targetWo.getTotalValue();

                BigDecimal installmentValue = newTotal.divide(new BigDecimal(installmentsCount), 2, RoundingMode.HALF_UP);
                BigDecimal sumPrevious = installmentValue.multiply(new BigDecimal(installmentsCount - 1));
                BigDecimal lastInstallment = newTotal.subtract(sumPrevious);

                for (int i = 0; i < installmentsCount; i++) {
                    AccountsReceivable ar = receivables.get(i);

                    if (i == 0) ar.setDueDate(targetWo.getInstallDate());
                    else ar.setDueDate(targetWo.getInstallDate().plusMonths(i));

                    if (!"cancelled".equals(targetWo.getStatus()) && "cancelled".equals(ar.getStatus())) {
                        ar.setStatus("pending");
                        ar.setNotes((ar.getNotes() != null ? ar.getNotes() : "") + "\n[Sistema] Reativado após edição da O.S.");
                    }

                    if (isTotalChanged) {
                        ar.setTotalAmount(i == installmentsCount - 1 ? lastInstallment : installmentValue);
                        String currentNotes = ar.getNotes() != null ? ar.getNotes() : "";
                        ar.setNotes(currentNotes + "\n[Sistema] " + changeReasonMsg);
                    }

                    receivableRepo.save(ar);
                }
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

            List<AccountsReceivable> receivables = receivableRepo.findAll().stream()
                    .filter(r -> r.getWorkOrder() != null && r.getWorkOrder().getId().equals(id))
                    .collect(Collectors.toList());

            for (AccountsReceivable ar : receivables) {
                ar.setStatus("cancelled");
                ar.setNotes((ar.getNotes() != null ? ar.getNotes() : "") + "\n[Sistema] Cancelado junto com a O.S.");
                receivableRepo.save(ar);
            }
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        WorkOrder wo = workOrderService.findById(id);

        if (wo != null) {
            if (wo.getQuote() != null) {
                Quote q = wo.getQuote();
                q.setWorkOrder(null);
                quoteRepository.saveAndFlush(q);
            }

            List<AccountsReceivable> receivables = receivableRepo.findAll().stream()
                    .filter(r -> r.getWorkOrder() != null && r.getWorkOrder().getId().equals(id))
                    .collect(Collectors.toList());
            receivableRepo.deleteAll(receivables);
            receivableRepo.flush();

            List<AccountsPayable> payables = payableRepo.findAll();
            for (AccountsPayable p : payables) {
                boolean changed = false;

                if (p.getWorkOrder() != null && p.getWorkOrder().getId().equals(id)) {
                    p.setWorkOrder(null);
                    changed = true;
                }

                if (p.getAllocations() != null && !p.getAllocations().isEmpty()) {
                    boolean removed = p.getAllocations().removeIf(alloc ->
                            alloc.getWorkOrder() != null && alloc.getWorkOrder().getId().equals(id)
                    );
                    if (removed) changed = true;
                }

                if (changed) {
                    payableRepo.saveAndFlush(p);
                }
            }
            workOrderService.delete(id);
        }

        return ResponseEntity.ok().build();
    }
}