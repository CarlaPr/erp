package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.FinanceService;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/receivables")
public class ReceivableController {

    private final AccountsReceivableRepository receivableRepository;
    private final ClientService clientService;
    private final WorkOrderService workOrderService;
    private final FinanceService financeService;

    public ReceivableController(AccountsReceivableRepository receivableRepository, ClientService clientService, WorkOrderService workOrderService, FinanceService financeService) {
        this.receivableRepository = receivableRepository;
        this.clientService = clientService;
        this.workOrderService = workOrderService;
        this.financeService = financeService;
    }

    @PostMapping("/pay/{id}")
    public String processPayment(@PathVariable UUID id, @RequestParam BigDecimal amount) {
        // Validação básica: não permitir valores negativos
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/receivables?error=invalid_amount";
        }

        financeService.processReceivablePayment(id, amount);
        return "redirect:/receivables?success=payment_processed";
    }

    @GetMapping
    public String index(Model model) {
        List<AccountsReceivable> list = receivableRepository.findAllByOrderByDueDateAsc();

        // KPIs
        BigDecimal faturado = list.stream().map(AccountsReceivable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recebido = list.stream().map(r -> r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal aReceber = faturado.subtract(recebido);
        BigDecimal emAtraso = list.stream()
                .filter(r -> "pending".equals(r.getStatus()) && r.getDueDate().isBefore(LocalDate.now()))
                .map(AccountsReceivable::getTotalAmount)
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

        return "receivables";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newReceivable") AccountsReceivable receivable) {
        // Se o valor recebido for igual ou maior que o total, muda o status para pago
        if(receivable.getReceivedAmount() != null && receivable.getReceivedAmount().compareTo(receivable.getTotalAmount()) >= 0) {
            receivable.setStatus("received");
        }
        receivableRepository.save(receivable);
        return "redirect:/receivables";
    }
}