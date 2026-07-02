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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final AccountsReceivableRepository accountsReceivableRepository;
    private final ReceiptService receiptService;

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService,
                               ServiceCategoryRepository categoryRepository, ProfileRepository profileRepository,
                               QuoteRepository quoteRepository, WorkOrderRepository workOrderRepo,
                               ClientRepository clientRepository, AccountsReceivableRepository accountsReceivableRepository, ReceiptService receiptService) { // Adicione no construtor
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.quoteRepository = quoteRepository;
        this.workOrderRepo = workOrderRepo;
        this.clientRepository = clientRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
        this.receiptService = receiptService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        List<WorkOrder> orders = workOrderRepo.findAllWithItemsOrderByCreatedAtDesc();

        // Carrega perfil
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
        model.addAttribute("clients", clientRepository.findAll()); // Busca direta no Repo
        model.addAttribute("availableQuotes", quoteRepository.findAll()); // Busca direta no Repo
        model.addAttribute("currentPage", "work-orders");

        return "work-orders";
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    @Transactional // NECESSÁRIO PARA MANTER A SESSÃO ABERTA
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
            targetWo.setTotalValue(workOrder.getTotalValue());
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
        return ResponseEntity.ok().build();
    }

    // Adicione este método ao WorkOrderController.java
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

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String detail(@PathVariable UUID id, Model model) {
        try {
            WorkOrder workOrder = workOrderRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Ordem de Serviço não encontrada"));

            Client client = workOrder.getClient();
            if (client == null) {
                throw new RuntimeException("Cliente não encontrado para esta OS");
            }

            List<WorkOrderItem> items = workOrder.getItems();
            if (items == null) {
                items = new ArrayList<>();
            }

            Quote quote = workOrder.getQuote();
            BigDecimal quoteValue = BigDecimal.ZERO;
            String quoteStatus = "N/A";

            if (quote != null) {
                quoteValue = quote.getTotalValue() != null ? quote.getTotalValue() : BigDecimal.ZERO;
                quoteStatus = quote.getStatus();
            }

            List<AccountsReceivable> receivables = new ArrayList<>();

            try {
                receivables = accountsReceivableRepository.findByWorkOrder(workOrder);
            } catch (Exception e) {
                System.out.println("Método findByWorkOrder não encontrado, usando query customizada");
            }

            BigDecimal totalItems = BigDecimal.ZERO;
            for (WorkOrderItem item : items) {
                if (item.getUnitPrice() != null && item.getQuantity() != null) {
                    long quantity = item.getQuantity().longValue();

                    BigDecimal itemTotal = item.getUnitPrice().multiply(
                            BigDecimal.valueOf(quantity)
                    );
                    totalItems = totalItems.add(itemTotal);
                }
            }

            BigDecimal totalReceived = BigDecimal.ZERO;
            BigDecimal totalPending = BigDecimal.ZERO;

            for (AccountsReceivable ar : receivables) {
                if (ar.getStatus() != null && ar.getStatus().equals("received")) {
                    BigDecimal receivedAmount = ar.getReceivedAmount() != null ? ar.getReceivedAmount() : BigDecimal.ZERO;
                    totalReceived = totalReceived.add(receivedAmount);
                } else if (ar.getStatus() != null && (ar.getStatus().equals("pending") || ar.getStatus().equals("partial"))) {
                    BigDecimal totalAmount = ar.getTotalAmount() != null ? ar.getTotalAmount() : BigDecimal.ZERO;
                    totalPending = totalPending.add(totalAmount);
                }
            }

            model.addAttribute("currentPage", "work-orders");
            model.addAttribute("workOrder", workOrder);
            model.addAttribute("client", client);
            model.addAttribute("items", items);
            model.addAttribute("quote", quote);
            model.addAttribute("quoteValue", quoteValue);
            model.addAttribute("quoteStatus", quoteStatus);
            model.addAttribute("receivables", receivables);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("totalReceived", totalReceived);
            model.addAttribute("totalPending", totalPending);

            String statusColor = getStatusColor(workOrder.getStatus());
            model.addAttribute("statusColor", statusColor);

            try {
                boolean hasReceipt = receiptService.hasReceipt(id);
                model.addAttribute("hasReceipt", hasReceipt);

                if (hasReceipt) {
                    List<Receipt> receipts = receiptService.getReceiptsByWorkOrder(id);
                    model.addAttribute("receipts", receipts);

                    long draftReceipts = receipts.stream()
                            .filter(r -> r.getStatus() != null && r.getStatus().equals("draft"))
                            .count();

                    long issuedReceipts = receipts.stream()
                            .filter(r -> r.getStatus() != null && (
                                    r.getStatus().equals("issued") ||
                                            r.getStatus().equals("printed") ||
                                            r.getStatus().equals("sent")))
                            .count();

                    model.addAttribute("draftReceipts", draftReceipts);
                    model.addAttribute("issuedReceipts", issuedReceipts);

                    if (!receipts.isEmpty()) {
                        Receipt latestReceipt = receipts.stream()
                                .max((r1, r2) -> {
                                    LocalDateTime d1 = r1.getCreatedAt() != null ? r1.getCreatedAt() : LocalDateTime.now();
                                    LocalDateTime d2 = r2.getCreatedAt() != null ? r2.getCreatedAt() : LocalDateTime.now();
                                    return d1.compareTo(d2);
                                })
                                .orElse(null);

                        model.addAttribute("latestReceipt", latestReceipt);
                    }
                } else {
                    model.addAttribute("receipts", new ArrayList<>());
                    model.addAttribute("draftReceipts", 0);
                    model.addAttribute("issuedReceipts", 0);
                    model.addAttribute("latestReceipt", null);
                }

            } catch (Exception e) {
                System.err.println("Erro ao buscar informações de recibos para OS " + id + ": " + e.getMessage());
                e.printStackTrace();

                model.addAttribute("receiptError", "Erro ao carregar dados de recibos: " + e.getMessage());
                model.addAttribute("hasReceipt", false);
                model.addAttribute("receipts", new ArrayList<>());
                model.addAttribute("draftReceipts", 0);
                model.addAttribute("issuedReceipts", 0);
                model.addAttribute("latestReceipt", null);
            }

            return "work-order-detail";

        } catch (RuntimeException e) {
            return "redirect:/work-orders?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    private String getStatusColor(String status) {
        if (status == null) return "slate";

        return switch (status) {
            case "draft" -> "slate";
            case "quoted" -> "blue";
            case "approved" -> "emerald";
            case "in_progress" -> "amber";
            case "completed" -> "green";
            case "cancelled" -> "red";
            default -> "slate";
        };
    }

}