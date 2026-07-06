package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final QuoteRepository quoteRepo;
    private final WorkOrderRepository woRepo;
    private final AccountsPayableRepository payableRepo;
    private final AccountsReceivableRepository receivableRepo;

    public ReportController(QuoteRepository quoteRepo, WorkOrderRepository woRepo, AccountsPayableRepository payableRepo, AccountsReceivableRepository receivableRepo) {
        this.quoteRepo = quoteRepo;
        this.woRepo = woRepo;
        this.payableRepo = payableRepo;
        this.receivableRepo = receivableRepo;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("currentPage", "reports");
        return "reports";
    }

    @GetMapping("/api/generate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> generateReport(@RequestParam String type, @RequestParam String month) {
        Map<String, Object> response = new HashMap<>();

        YearMonth ym = YearMonth.parse(month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        if ("quotes".equals(type)) {
            List<Quote> quotes = quoteRepo.findAll().stream()
                    .filter(q -> q.getDateCreated() != null && !q.getDateCreated().isBefore(start) && !q.getDateCreated().isAfter(end))
                    .collect(Collectors.toList());

            response.put("totalCount", quotes.size());
            response.put("approvedCount", quotes.stream().filter(q -> "approved".equals(q.getStatus())).count());

            double totalVal = quotes.stream().mapToDouble(q -> q.getTotalValue() != null ? ((Number)q.getTotalValue()).doubleValue() : 0.0).sum();
            double approvedVal = quotes.stream().filter(q -> "approved".equals(q.getStatus())).mapToDouble(q -> q.getTotalValue() != null ? ((Number)q.getTotalValue()).doubleValue() : 0.0).sum();
            response.put("totalValue", totalVal);
            response.put("approvedValue", approvedVal);

            List<Map<String, Object>> items = quotes.stream().map(q -> {
                Map<String, Object> map = new HashMap<>();
                map.put("numero", q.getNumber());
                map.put("cliente", q.getClient() != null ? q.getClient().getName() : "Consumidor Final");
                map.put("status", q.getStatus());
                map.put("valor", q.getTotalValue());
                map.put("data", q.getDateCreated().toLocalDate().toString());
                return map;
            }).collect(Collectors.toList());
            response.put("items", items);

        } else if ("work_orders".equals(type)) {
            List<WorkOrder> wos = woRepo.findAll().stream()
                    .filter(w -> w.getCreatedAt() != null && !w.getCreatedAt().isBefore(start) && !w.getCreatedAt().isAfter(end))
                    .collect(Collectors.toList());

            response.put("totalCount", wos.size());
            response.put("completedCount", wos.stream().filter(w -> "completed".equals(w.getStatus())).count());
            response.put("totalValue", wos.stream().mapToDouble(w -> w.getTotalValue() != null ? ((Number)w.getTotalValue()).doubleValue() : 0.0).sum());

            List<Map<String, Object>> items = wos.stream().map(w -> {
                Map<String, Object> map = new HashMap<>();
                map.put("numero", w.getNumber());
                map.put("cliente", w.getClient() != null ? w.getClient().getName() : "Sem Cliente");
                map.put("status", w.getStatus());
                map.put("valor", w.getTotalValue());
                map.put("data", w.getCreatedAt().toLocalDate().toString());
                return map;
            }).collect(Collectors.toList());
            response.put("items", items);

        } else if ("financial".equals(type)) {
            List<AccountsReceivable> receitas = receivableRepo.findAll().stream()
                    .filter(r -> r.getDueDate() != null && !r.getDueDate().isBefore(startDate) && !r.getDueDate().isAfter(endDate))
                    .collect(Collectors.toList());

            List<AccountsPayable> despesas = payableRepo.findAll().stream()
                    .filter(p -> p.getDueDate() != null && !p.getDueDate().isBefore(startDate) && !p.getDueDate().isAfter(endDate))
                    .collect(Collectors.toList());

            double totalReceitas = receitas.stream().mapToDouble(r -> r.getTotalAmount() != null ? ((Number)r.getTotalAmount()).doubleValue() : 0.0).sum();
            double receitasPagas = receitas.stream().filter(r -> "paid".equalsIgnoreCase(r.getStatus())).mapToDouble(r -> r.getReceivedAmount() != null ? ((Number)r.getReceivedAmount()).doubleValue() : 0.0).sum();
            double totalDespesas = despesas.stream().mapToDouble(p -> p.getTotalAmount() != null ? ((Number)p.getTotalAmount()).doubleValue() : 0.0).sum();
            double despesasPagas = despesas.stream().filter(p -> "paid".equalsIgnoreCase(p.getStatus())).mapToDouble(p -> p.getPaidAmount() != null ? ((Number)p.getPaidAmount()).doubleValue() : 0.0).sum();

            response.put("totalReceitas", totalReceitas);
            response.put("receitasPagas", receitasPagas);
            response.put("totalDespesas", totalDespesas);
            response.put("despesasPagas", despesasPagas);
            response.put("lucroProjetado", totalReceitas - totalDespesas);
            response.put("lucroRealizado", receitasPagas - despesasPagas);

            List<Map<String, Object>> recItems = receitas.stream().map(r -> {
                Map<String, Object> map = new HashMap<>();
                map.put("tipo", "RECEITA");
                map.put("descricao", r.getDescription());
                map.put("cliente", r.getClient() != null ? r.getClient().getName() : "-");
                map.put("valor", r.getTotalAmount()); // CORREÇÃO: getTotalAmount()
                map.put("vencimento", r.getDueDate().toString());
                map.put("status", r.getStatus());
                return map;
            }).collect(Collectors.toList());

            List<Map<String, Object>> despItems = despesas.stream().map(d -> {
                Map<String, Object> map = new HashMap<>();
                map.put("tipo", "DESPESA");
                map.put("descricao", d.getDescription());
                map.put("fornecedor", d.getSupplier() != null ? d.getSupplier().getName() : "-");
                map.put("valor", d.getTotalAmount()); // CORREÇÃO: getTotalAmount()
                map.put("vencimento", d.getDueDate().toString());
                map.put("status", d.getStatus());
                return map;
            }).collect(Collectors.toList());

            List<Map<String, Object>> allItems = new ArrayList<>();
            allItems.addAll(recItems);
            allItems.addAll(despItems);

            allItems.sort((a, b) -> ((String)a.get("vencimento")).compareTo((String)b.get("vencimento")));
            response.put("items", allItems);
        }

        return ResponseEntity.ok(response);
    }
}