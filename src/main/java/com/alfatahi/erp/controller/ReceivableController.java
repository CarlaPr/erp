package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.ReceiptService;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/receivables")
public class ReceivableController {

    private final AccountsReceivableRepository receivableRepository;
    private final ClientService clientService;
    private final WorkOrderService workOrderService;
    private final FinanceService financeService;
    private final ClientRepository clientRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ReceiptService receiptService;

    public ReceivableController(AccountsReceivableRepository receivableRepository,
                                ClientService clientService,
                                WorkOrderService workOrderService,
                                FinanceService financeService,
                                ClientRepository clientRepository,
                                WorkOrderRepository workOrderRepository, ReceiptService receiptService) {
        this.receivableRepository = receivableRepository;
        this.clientService = clientService;
        this.workOrderService = workOrderService;
        this.financeService = financeService;
        this.clientRepository = clientRepository;
        this.workOrderRepository = workOrderRepository;
        this.receiptService = receiptService;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) UUID workOrderId,
            Model model) {

        List<AccountsReceivable> list = receivableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(r -> !"cancelled".equals(r.getStatus()) || "cancelled".equals(status))
                .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream().filter(r ->
                    r.getDescription().toLowerCase().contains(q)
                            || (r.getClient() != null && r.getClient().getName().toLowerCase().contains(q))
                            || (r.getWorkOrder() != null && r.getWorkOrder().getNumber().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(r -> status.equals(r.getStatus())).collect(Collectors.toList());
        }
        if (clientId != null) {
            list = list.stream().filter(r -> r.getClient() != null && clientId.equals(r.getClient().getId())).collect(Collectors.toList());
        }
        if (dateFrom != null) {
            list = list.stream().filter(r -> !r.getDueDate().isBefore(dateFrom)).collect(Collectors.toList());
        }
        if (dateTo != null) {
            list = list.stream().filter(r -> !r.getDueDate().isAfter(dateTo)).collect(Collectors.toList());
        }
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            list = list.stream().filter(r -> paymentMethod.equals(r.getPaymentMethod())).collect(Collectors.toList());
        }
        if (workOrderId != null) {
            list = list.stream().filter(r -> r.getWorkOrder() != null && workOrderId.equals(r.getWorkOrder().getId())).collect(Collectors.toList());
        }

        List<AccountsReceivable> all = receivableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(r -> !"cancelled".equals(r.getStatus()))
                .collect(Collectors.toList());

        BigDecimal faturado = all.stream().map(AccountsReceivable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recebido = all.stream().map(AccountsReceivable::getNetReceivedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal aReceber = all.stream()
                .filter(r -> "pending".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                .map(AccountsReceivable::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal emAtraso = all.stream()
                .filter(r -> ("pending".equals(r.getStatus()) || "partial".equals(r.getStatus())) && r.getDueDate().isBefore(LocalDate.now()))
                .map(AccountsReceivable::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentPage", "receivables");
        model.addAttribute("receivables", list);
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("workOrders", workOrderService.listAll());
        model.addAttribute("newReceivable", new AccountsReceivable());
        model.addAttribute("valFaturado", faturado);
        model.addAttribute("valRecebido", recebido);
        model.addAttribute("valAReceber", aReceber);
        model.addAttribute("valAtraso", emAtraso);
        // Filtros
        model.addAttribute("filterSearch", search);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterClientId", clientId);
        model.addAttribute("filterDateFrom", dateFrom);
        model.addAttribute("filterDateTo", dateTo);
        model.addAttribute("filterPaymentMethod", paymentMethod);
        model.addAttribute("filterWorkOrderId", workOrderId);

        return "receivables";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newReceivable") AccountsReceivable receivable) {
        if (receivable.getClient() != null && receivable.getClient().getId() != null) {
            receivable.setClient(clientService.findById(receivable.getClient().getId()));
        } else {
            receivable.setClient(null);
        }

        if (receivable.getWorkOrder() != null && receivable.getWorkOrder().getId() != null) {
            receivable.setWorkOrder(workOrderService.findById(receivable.getWorkOrder().getId()));
        } else {
            receivable.setWorkOrder(null);
        }

        if (receivable.getReceivedAmount() == null) receivable.setReceivedAmount(BigDecimal.ZERO);
        if (receivable.getInstallments() == null) receivable.setInstallments(1);
        if (receivable.getDiscount() == null) receivable.setDiscount(BigDecimal.ZERO);
        if (receivable.getCardFeePercentage() == null) receivable.setCardFeePercentage(BigDecimal.ZERO);

        receivable.setStatus("pending");
        receivableRepository.save(receivable);

        return "redirect:/receivables";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable UUID id, @ModelAttribute AccountsReceivable form) {
        AccountsReceivable ar = receivableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        ar.setDescription(form.getDescription());
        ar.setTotalAmount(form.getTotalAmount());
        ar.setDueDate(form.getDueDate());
        ar.setInstallments(form.getInstallments());
        ar.setCardFeePercentage(form.getCardFeePercentage());
        ar.setDiscount(form.getDiscount());
        ar.setNotes(form.getNotes());

        if (form.getClient() != null && form.getClient().getId() != null) {
            ar.setClient(clientService.findById(form.getClient().getId()));
        } else {
            ar.setClient(null);
        }

        if (form.getWorkOrder() != null && form.getWorkOrder().getId() != null) {
            ar.setWorkOrder(workOrderService.findById(form.getWorkOrder().getId()));
        } else {
            ar.setWorkOrder(null);
        }
        receivableRepository.save(ar);
        return "redirect:/receivables";
    }

    @PostMapping("/pay/{id}")
    @Transactional
    public String processPayment(
            @PathVariable UUID id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) BigDecimal discount,
            @RequestParam(required = false) BigDecimal cardFee,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        try {
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Valor inválido");
                return "redirect:/receivables";
            }

            AccountsReceivable ar = receivableRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Conta a receber não encontrada"));

            financeService.processReceivablePayment(id, amount, paymentDate, cardFee, notes);

            ar = receivableRepository.findById(id).get();

            if (paymentMethod != null && !paymentMethod.isBlank()) {
                ar.setPaymentMethod(paymentMethod);
            }
            if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
                ar.setDiscount(discount);
            }
            if (cardFee != null && cardFee.compareTo(BigDecimal.ZERO) > 0) {
                ar.setCardFeePercentage(cardFee);
            }

            receivableRepository.save(ar);

            if (ar.getStatus().equals("received")) {
                try {
                    receiptService.createReceipt(ar.getId());

                    System.out.println("✨ Recibo criado automaticamente para conta: " + ar.getId());

                } catch (Exception e) {
                    System.err.println("⚠️  Erro ao criar recibo automático: " + e.getMessage());

                    redirectAttributes.addFlashAttribute("warning",
                            "Pagamento processado, mas houve erro ao criar recibo: " + e.getMessage());
                }
            }

            redirectAttributes.addFlashAttribute("success", "Pagamento processado com sucesso!");
            return "redirect:/receivables";

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Erro ao processar pagamento: " + e.getMessage());
            return "redirect:/receivables";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Erro inesperado: " + e.getMessage());
            return "redirect:/receivables";
        }
    }

    @PostMapping("/cancel/{id}")
    public String cancel(@PathVariable UUID id) {
        AccountsReceivable ar = receivableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        ar.setStatus("cancelled");
        receivableRepository.save(ar);
        return "redirect:/receivables";
    }


    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        return cancel(id);
    }

    @PostMapping("/{id}/update")
    public String updateReceivable(
            @PathVariable UUID id,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String paymentMethod,
            RedirectAttributes redirectAttributes) {

        try {
            AccountsReceivable ar = receivableRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Conta não encontrada"));

            if (notes != null && !notes.isBlank()) {
                ar.setNotes(notes);
            }
            if (paymentMethod != null && !paymentMethod.isBlank()) {
                ar.setPaymentMethod(paymentMethod);
            }

            receivableRepository.save(ar);
            redirectAttributes.addFlashAttribute("success", "Conta atualizada com sucesso");
            return "redirect:/receivables/" + id;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao atualizar: " + e.getMessage());
            return "redirect:/receivables";
        }
    }

    @Transactional(readOnly = true)
    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) UUID workOrderId,
            jakarta.servlet.http.HttpServletResponse response) throws Exception {

        List<AccountsReceivable> list = receivableRepository.findAllByOrderByDueDateAsc().stream()
                .filter(r -> !"cancelled".equals(r.getStatus()) || "cancelled".equals(status))
                .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            list = list.stream().filter(r ->
                    r.getDescription().toLowerCase().contains(q)
                            || (r.getClient() != null && r.getClient().getName().toLowerCase().contains(q))
                            || (r.getWorkOrder() != null && r.getWorkOrder().getNumber().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }
        if (status != null && !status.isBlank()) list = list.stream().filter(r -> status.equals(r.getStatus())).collect(Collectors.toList());
        if (clientId != null) list = list.stream().filter(r -> r.getClient() != null && clientId.equals(r.getClient().getId())).collect(Collectors.toList());
        if (dateFrom != null) list = list.stream().filter(r -> !r.getDueDate().isBefore(dateFrom)).collect(Collectors.toList());
        if (dateTo != null) list = list.stream().filter(r -> !r.getDueDate().isAfter(dateTo)).collect(Collectors.toList());
        if (paymentMethod != null && !paymentMethod.isBlank()) list = list.stream().filter(r -> paymentMethod.equals(r.getPaymentMethod())).collect(Collectors.toList());
        if (workOrderId != null) list = list.stream().filter(r -> r.getWorkOrder() != null && workOrderId.equals(r.getWorkOrder().getId())).collect(Collectors.toList());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"contas_a_receber.csv\"");
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(response.getOutputStream(), "UTF-8"));

        writer.println("Vencimento;Data Pagamento;Cliente;O.S.;Descricao;Forma Pgto;Taxa Cartao (%);Desconto (R$);Total;Recebido;Pendente;Status");

        java.util.Locale ptBR = new java.util.Locale("pt", "BR");

        for (AccountsReceivable r : list) {
            String clientName = r.getClient() != null ? r.getClient().getName() : "Avulso";
            String osNumber = r.getWorkOrder() != null ? r.getWorkOrder().getNumber() : "-";
            String desc = r.getDescription() != null ? r.getDescription().replace(";", ",") : "";

            String formPgto = r.getPaymentMethod() != null ? r.getPaymentMethod() : "";
            BigDecimal taxa = r.getCardFeePercentage() != null ? r.getCardFeePercentage() : BigDecimal.ZERO;
            BigDecimal desconto = r.getDiscount() != null ? r.getDiscount() : BigDecimal.ZERO;

            String payDate = r.getPaymentDate() != null ? r.getPaymentDate().toString() : "";

            writer.printf(ptBR, "%s;%s;%s;%s;%s;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%s\n",
                    r.getDueDate(), payDate, clientName, osNumber, desc, formPgto,
                    taxa, desconto, r.getTotalAmount(), r.getNetReceivedAmount(), r.getBalance(), r.getStatus());
        }
        writer.flush();
    }
}