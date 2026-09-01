package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.WorkOrderPaymentStatusDto;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.*;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.Hibernate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
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
    private final QuoteService quoteService;

    public WorkOrderController(WorkOrderService workOrderService, ClientService clientService,
                               ServiceCategoryRepository categoryRepository, ProfileRepository profileRepository,
                               QuoteRepository quoteRepository, WorkOrderRepository workOrderRepo,
                               ClientRepository clientRepository, AccountsReceivableRepository receivableRepo,
                               AccountsPayableRepository payableRepo, QuoteService quoteService) {
        this.workOrderService = workOrderService;
        this.clientService = clientService;
        this.categoryRepository = categoryRepository;
        this.profileRepository = profileRepository;
        this.quoteRepository = quoteRepository;
        this.workOrderRepo = workOrderRepo;
        this.clientRepository = clientRepository;
        this.receivableRepo = receivableRepo;
        this.payableRepo = payableRepo;
        this.quoteService = quoteService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        List<WorkOrder> orders = workOrderRepo.findAllWithItemsOrderByCreatedAtDesc();

        Profile profile = profileRepository.findAll().stream().findFirst().orElseGet(() -> {
            Profile p = new Profile();
            p.setCompanyName("Alfa Tahi");
            return profileRepository.save(p);
        });

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal globalProfit = BigDecimal.ZERO;
        BigDecimal averageMargin = BigDecimal.ZERO;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTecnico = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("TECNICO") || a.getAuthority().equals("ROLE_TECNICO"));

        if (!isTecnico) {
            totalRevenue = orders.stream()
                    .filter(wo -> !"cancelled".equals(wo.getStatus()) && !"canceled".equals(wo.getStatus()))
                    .map(wo -> wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalCost = orders.stream()
                    .filter(wo -> !"cancelled".equals(wo.getStatus()) && !"canceled".equals(wo.getStatus()))
                    .map(wo -> wo.getTotalCost() != null ? wo.getTotalCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            globalProfit = totalRevenue.subtract(totalCost);

            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
                averageMargin = globalProfit.multiply(new BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP);
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("approvedWithoutWorkOrder", quoteRepository.findApprovedWithoutWorkOrder());
        model.addAttribute("paymentStatusByOrder", buildPaymentStatusMap(orders));


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

    @PostMapping("/recover/{quoteId}")
    @ResponseBody
    public ResponseEntity<?> recover(@PathVariable UUID quoteId) {
        WorkOrder order = quoteService.recoverApprovedWorkOrder(quoteId);
        return ResponseEntity.ok(Map.of("id", order.getId(), "number", order.getNumber()));
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public ResponseEntity<String> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(409).body(exception.getMessage());
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> saveAjax(@RequestBody WorkOrder workOrder) {
        WorkOrder targetWo;
        BigDecimal oldTotal = null;
        String changeReasonMsg = "Valor ajustado conforme alteração na O.S.";
        List<WorkOrderItem> incomingItems = workOrder.getItems() != null
                ? new ArrayList<>(workOrder.getItems()) : new ArrayList<>();
        Quote linkedQuote = null;
        if (workOrder.getQuoteId() != null) {
            linkedQuote = quoteRepository.findByIdForUpdate(workOrder.getQuoteId())
                    .orElseThrow(() -> new IllegalStateException("Orçamento não encontrado."));
            WorkOrder linkedOrder = workOrderRepo.findByQuoteId(linkedQuote.getId()).orElse(null);
            if (linkedOrder != null && !linkedOrder.getId().equals(workOrder.getId())) {
                throw new IllegalStateException("Este orçamento já possui a O.S. " + linkedOrder.getNumber() + ". Edite a ordem existente.");
            }
            if (workOrder.getId() == null && "approved".equals(linkedQuote.getStatus())) {
                throw new IllegalStateException("Use Recuperar O.S. na lista de orçamentos aprovados sem O.S. para preservar os dados da aprovação.");
            }
        }

        if (workOrder.getId() != null) {
            targetWo = workOrderService.findById(workOrder.getId());
            if (targetWo == null) return ResponseEntity.notFound().build();

            if (targetWo.getQuote() != null && linkedQuote != null
                    && !targetWo.getQuote().getId().equals(linkedQuote.getId())) {
                throw new IllegalStateException("O orçamento de origem desta O.S. não pode ser trocado.");
            }

            oldTotal = targetWo.getTotalValue();

            targetWo.setTitle(workOrder.getTitle());
            targetWo.setStatus(workOrder.getStatus());
            targetWo.setDescription(workOrder.getDescription());

            targetWo.setNotes(workOrder.getNotes());

            BigDecimal newTotal = workOrder.getTotalValue();
            if (newTotal != null && newTotal.compareTo(BigDecimal.ZERO) > 0) {
                targetWo.setTotalValue(newTotal);
            } else {
                targetWo.setTotalValue(workOrderService.calculateTotalValueFromItems(workOrder));
            }

            if (oldTotal != null && targetWo.getTotalValue().compareTo(oldTotal) != 0) {
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
        } else {
            targetWo = workOrder;
            if (targetWo.getStatus() == null || targetWo.getStatus().isBlank()) {
                targetWo.setStatus("pending");
            }
        }

        if (targetWo.getItems() == null) targetWo.setItems(new ArrayList<>());
        targetWo.getItems().clear();
        for (WorkOrderItem item : incomingItems) {
            item.setWorkOrder(targetWo);
            targetWo.getItems().add(item);
        }

        if (linkedQuote != null) {
            targetWo.setQuote(linkedQuote);
            linkedQuote.setWorkOrder(targetWo);
        }

        workOrderService.save(targetWo);
        workOrderService.redistributeReceivablesIfTotalChanged(targetWo, oldTotal, changeReasonMsg);

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

            // O cancelamento mantém a origem da venda e seu histórico.
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
            // Valida antes de remover vínculos ou registros financeiros.
            workOrderService.validateDeletion(wo);
            if (wo.getQuote() != null) {
                Quote q = wo.getQuote();
                q.setWorkOrder(null);
                quoteRepository.saveAndFlush(q);
            }

            List<AccountsReceivable> receivables = receivableRepo.findAll().stream()
                    .filter(r -> r.getWorkOrder() != null && r.getWorkOrder().getId().equals(id))
                    .collect(Collectors.toList());

            List<UUID> receivableIds = receivables.stream()
                    .map(AccountsReceivable::getId)
                    .collect(Collectors.toList());

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

                if (p.getSourceReceivableId() != null && receivableIds.contains(p.getSourceReceivableId())) {
                    p.setSourceReceivableId(null);
                    changed = true;
                }

                if (changed) {
                    payableRepo.saveAndFlush(p);
                }
            }

            receivableRepo.deleteAll(receivables);
            receivableRepo.flush();

            workOrderService.delete(id);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Calcula, para cada O.S. da lista, um resumo do status de pagamento (contas a
     * receber vinculadas), para exibição individual no card de cada O.S.
     */
    private Map<UUID, WorkOrderPaymentStatusDto> buildPaymentStatusMap(List<WorkOrder> orders) {
        if (orders == null || orders.isEmpty()) return Collections.emptyMap();

        List<UUID> orderIds = orders.stream().map(WorkOrder::getId).collect(Collectors.toList());

        List<AccountsReceivable> receivables = receivableRepo.findByWorkOrderIdIn(orderIds).stream()
                .filter(r -> !"cancelled".equals(r.getStatus()))
                .collect(Collectors.toList());

        Map<UUID, List<AccountsReceivable>> byOrder = receivables.stream()
                .filter(r -> r.getWorkOrder() != null)
                .collect(Collectors.groupingBy(r -> r.getWorkOrder().getId()));

        Map<UUID, WorkOrderPaymentStatusDto> result = new HashMap<>();
        for (WorkOrder wo : orders) {
            result.put(wo.getId(), computePaymentStatus(byOrder.getOrDefault(wo.getId(), Collections.emptyList())));
        }
        return result;
    }

    private static final String BADGE_PAGO = "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/20";
    private static final String BADGE_ENTRADA = "bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-500/10 dark:text-blue-300 dark:border-blue-500/20";
    private static final String BADGE_ENTREGA = "bg-violet-50 text-violet-700 border-violet-200 dark:bg-violet-500/10 dark:text-violet-300 dark:border-violet-500/20";
    private static final String BADGE_PARCIAL = "bg-amber-50 text-amber-700 border-amber-200 dark:bg-amber-500/10 dark:text-amber-300 dark:border-amber-500/20";
    private static final String BADGE_PENDENTE = "bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-700/40 dark:text-slate-300 dark:border-slate-600/50";

    private WorkOrderPaymentStatusDto computePaymentStatus(List<AccountsReceivable> receivables) {
        if (receivables == null || receivables.isEmpty()) {
            return new WorkOrderPaymentStatusDto("Pagamento pendente", BADGE_PENDENTE);
        }

        AccountsReceivable unico = receivables.stream()
                .filter(r -> "recebimento_total".equals(r.getPaymentStage()) || "unico".equals(r.getPaymentStage()))
                .findFirst().orElse(null);

        if (unico != null) {
            if ("received".equals(unico.getStatus())) {
                return new WorkOrderPaymentStatusDto("Pago integral", BADGE_PAGO);
            }
            if ("partial".equals(unico.getStatus())) {
                return new WorkOrderPaymentStatusDto("Pagamento parcial", BADGE_PARCIAL);
            }
            return new WorkOrderPaymentStatusDto("Pagamento pendente", BADGE_PENDENTE);
        }

        AccountsReceivable entrada = receivables.stream().filter(r -> "entrada".equals(r.getPaymentStage())).findFirst().orElse(null);
        AccountsReceivable entrega = receivables.stream().filter(r -> "entrega".equals(r.getPaymentStage())).findFirst().orElse(null);

        boolean entradaPaga = entrada != null && "received".equals(entrada.getStatus());
        boolean entregaPaga = entrega != null && "received".equals(entrega.getStatus());
        boolean algumParcial = receivables.stream().anyMatch(r -> "partial".equals(r.getStatus()));

        if (entradaPaga && entregaPaga) {
            return new WorkOrderPaymentStatusDto("Pago integral", BADGE_PAGO);
        }
        if (entradaPaga) {
            return new WorkOrderPaymentStatusDto("Pago entrada", BADGE_ENTRADA);
        }
        if (entregaPaga) {
            return new WorkOrderPaymentStatusDto("Pago entrega", BADGE_ENTREGA);
        }
        if (algumParcial) {
            return new WorkOrderPaymentStatusDto("Pagamento parcial", BADGE_PARCIAL);
        }
        return new WorkOrderPaymentStatusDto("Pagamento pendente", BADGE_PENDENTE);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResponseEntity<String> handleDataIntegrityConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body("Conflito ao salvar a O.S. (possível número duplicado). Tente novamente.");
    }
}