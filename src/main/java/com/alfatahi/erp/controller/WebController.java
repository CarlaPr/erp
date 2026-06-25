package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.DashboardDto;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Controller
public class WebController {

    private final AccountsPayableRepository payableRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final WorkOrderService workOrderService;

    public WebController(AccountsPayableRepository payableRepository, AccountsReceivableRepository receivableRepository, WorkOrderService workOrderService) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.workOrderService = workOrderService;
    }

    @GetMapping("/")
    public String root() { return "redirect:/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDto dto = new DashboardDto();
        List<AccountsReceivable> receivables = receivableRepository.findAllByOrderByDueDateAsc();
        List<AccountsPayable> payables = payableRepository.findAllByOrderByDueDateAsc();
        List<WorkOrder> workOrders = workOrderService.listAll();
        LocalDate today = LocalDate.now();

        // ==========================================
        // 1. CÁLCULOS DOS CARDS (KPIs REAIS)
        // ==========================================
        BigDecimal faturado = receivables.stream().map(AccountsReceivable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recebido = receivables.stream().map(r -> r.getReceivedAmount() != null ? r.getReceivedAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setaReceber(faturado.subtract(recebido));
        dto.setRecebido(recebido);

        BigDecimal cadastrado = payables.stream().map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pago = payables.stream().filter(p -> "paid".equals(p.getStatus())).map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setaPagar(cadastrado.subtract(pago));
        dto.setPago(pago);

        dto.setSaldoAtual(recebido.subtract(pago));
        dto.setSaldoProjetado(dto.getSaldoAtual().add(dto.getaReceber()).subtract(dto.getaPagar()));

        long atrasados = receivables.stream().filter(r -> "pending".equals(r.getStatus()) && r.getDueDate() != null && r.getDueDate().isBefore(today)).count();
        dto.setInadimplentes(atrasados);

        long osAtivas = workOrders.stream().filter(wo -> !"completed".equals(wo.getStatus()) && !"cancelled".equals(wo.getStatus())).count();
        dto.setOsAtivas(osAtivas);

        BigDecimal receitaTotalOs = workOrders.stream().map(WorkOrder::getTotalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!workOrders.isEmpty()) {
            dto.setTicketMedio(receitaTotalOs.divide(new BigDecimal(workOrders.size()), 2, RoundingMode.HALF_UP));
        }

        // ==========================================
        // 2. VISÃO 1: FLUXO DE CAIXA (12 MESES REAIS)
        // ==========================================
        int currentYear = today.getYear();
        BigDecimal[] monthRevenues = new BigDecimal[12];
        BigDecimal[] monthExpenses = new BigDecimal[12];
        Arrays.fill(monthRevenues, BigDecimal.ZERO);
        Arrays.fill(monthExpenses, BigDecimal.ZERO);

        for (AccountsReceivable r : receivables) {
            if (r.getDueDate() != null && r.getDueDate().getYear() == currentYear) {
                int monthIndex = r.getDueDate().getMonthValue() - 1;
                monthRevenues[monthIndex] = monthRevenues[monthIndex].add(r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO);
            }
        }

        for (AccountsPayable p : payables) {
            if (p.getDueDate() != null && p.getDueDate().getYear() == currentYear) {
                int monthIndex = p.getDueDate().getMonthValue() - 1;
                monthExpenses[monthIndex] = monthExpenses[monthIndex].add(p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO);
            }
        }

        List<Map<String, Object>> monthlyFlow = new ArrayList<>();
        String[] monthNames = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
        for(int i = 0; i < 12; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("month", monthNames[i]);
            map.put("in", monthRevenues[i]);
            map.put("out", monthExpenses[i]);
            monthlyFlow.add(map);
        }

        // ==========================================
        // 3. VISÃO 2: CUSTOS POR CATEGORIA
        // ==========================================
        BigDecimal costVar = BigDecimal.ZERO;
        BigDecimal costFix = BigDecimal.ZERO;
        BigDecimal costProv = BigDecimal.ZERO;

        for (AccountsPayable p : payables) {
            BigDecimal val = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;
            if ("variable".equals(p.getCategory())) costVar = costVar.add(val);
            else if ("fixed".equals(p.getCategory())) costFix = costFix.add(val);
            else if ("provision".equals(p.getCategory())) costProv = costProv.add(val);
        }

        BigDecimal totalCosts = costVar.add(costFix).add(costProv);
        BigDecimal pctVar = totalCosts.compareTo(BigDecimal.ZERO) > 0 ? costVar.multiply(new BigDecimal("100")).divide(totalCosts, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal pctFix = totalCosts.compareTo(BigDecimal.ZERO) > 0 ? costFix.multiply(new BigDecimal("100")).divide(totalCosts, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal pctProv = totalCosts.compareTo(BigDecimal.ZERO) > 0 ? costProv.multiply(new BigDecimal("100")).divide(totalCosts, 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // ==========================================
        // 4. VISÃO 3: PERFORMANCE POR TIPO DE SERVIÇO
        // ==========================================
        Map<String, BigDecimal> revenueByCat = new HashMap<>();
        Map<String, BigDecimal> profitByCat = new HashMap<>();
        BigDecimal lucroBrutoTotalGeral = BigDecimal.ZERO;

        for (WorkOrder wo : workOrders) {
            BigDecimal receita = wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO;
            BigDecimal custo = workOrderService.calculateObraCost(wo.getId());
            if(custo == null) custo = BigDecimal.ZERO;
            BigDecimal lucro = receita.subtract(custo);

            if (!"cancelled".equals(wo.getStatus())) {
                lucroBrutoTotalGeral = lucroBrutoTotalGeral.add(lucro);
            }

            if ("completed".equals(wo.getStatus())) {
                String catName = (wo.getCategory() != null && wo.getCategory().getName() != null) ? wo.getCategory().getName() : "Diversos";
                revenueByCat.put(catName, revenueByCat.getOrDefault(catName, BigDecimal.ZERO).add(receita));
                profitByCat.put(catName, profitByCat.getOrDefault(catName, BigDecimal.ZERO).add(lucro));
            }
        }

        dto.setLucroBruto(lucroBrutoTotalGeral);
        if (receitaTotalOs.compareTo(BigDecimal.ZERO) > 0) {
            dto.setMargemMedia(lucroBrutoTotalGeral.multiply(new BigDecimal("100")).divide(receitaTotalOs, 2, RoundingMode.HALF_UP));
        }

        List<String> catLabels = new ArrayList<>(revenueByCat.keySet());
        List<BigDecimal> catRevenues = new ArrayList<>();
        List<BigDecimal> catProfits = new ArrayList<>();
        for(String cat : catLabels) {
            catRevenues.add(revenueByCat.get(cat));
            catProfits.add(profitByCat.get(cat));
        }

        // ==========================================
        // 5. ALERTAS DINÂMICOS
        // ==========================================
        List<AccountsReceivable> allPendingReceivables = receivables.stream()
                .filter(r -> "pending".equals(r.getStatus())).toList();

        List<AccountsPayable> allPendingPayables = payables.stream()
                .filter(p -> "pending".equals(p.getStatus())).toList();

        List<AccountsReceivable> overdueReceivables = allPendingReceivables.stream()
                .filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(today)).toList();

        List<AccountsPayable> overduePayables = allPendingPayables.stream()
                .filter(p -> p.getDueDate() != null && p.getDueDate().isBefore(today)).toList();

        List<WorkOrder> expiringWorkOrders = workOrders.stream()
                .filter(wo -> !"completed".equals(wo.getStatus()) && !"cancelled".equals(wo.getStatus()))
                .filter(wo -> wo.getInstallDate() != null && !wo.getInstallDate().isAfter(today.plusDays(3)))
                .toList();

        // ==========================================
        // 6. CALENDÁRIO UNIFICADO (ENTRADAS E SAÍDAS)
        // ==========================================
        List<Map<String, Object>> calendarEvents = new ArrayList<>();

        for (AccountsReceivable r : allPendingReceivables) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("type", "IN");
            ev.put("description", r.getDescription() != null ? r.getDescription() : (r.getClient() != null ? "Recebimento: " + r.getClient().getName() : "Entrada"));
            ev.put("date", r.getDueDate());
            ev.put("amount", r.getTotalAmount());
            calendarEvents.add(ev);
        }

        for (AccountsPayable p : allPendingPayables) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("type", "OUT");
            ev.put("description", p.getDescription() != null ? p.getDescription() : "Despesa");
            ev.put("date", p.getDueDate());
            ev.put("amount", p.getTotalAmount());
            calendarEvents.add(ev);
        }

        // Ordenar por data (os mais próximos ou atrasados primeiro)
        calendarEvents.sort((a, b) -> {
            LocalDate d1 = (LocalDate) a.get("date");
            LocalDate d2 = (LocalDate) b.get("date");
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });


        // ==========================================
        // 7. INJEÇÃO NO HTML
        // ==========================================
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("dash", dto);

        // Alertas
        model.addAttribute("allPendingReceivables", allPendingReceivables);
        model.addAttribute("allPendingPayables", allPendingPayables);
        model.addAttribute("overdueReceivables", overdueReceivables);
        model.addAttribute("overduePayables", overduePayables);
        model.addAttribute("expiringWorkOrders", expiringWorkOrders);

        // Calendário (Limitado a 16 para não quebrar infinito, mas exibe entradas e saídas)
        model.addAttribute("calendarEvents", calendarEvents.stream().limit(16).toList());

        model.addAttribute("monthlyFlow", monthlyFlow);
        model.addAttribute("costVar", costVar);
        model.addAttribute("costFix", costFix);
        model.addAttribute("costProv", costProv);
        model.addAttribute("pctVar", pctVar);
        model.addAttribute("pctFix", pctFix);
        model.addAttribute("pctProv", pctProv);

        model.addAttribute("barLabels", catLabels);
        model.addAttribute("barRevenues", catRevenues);
        model.addAttribute("barProfits", catProfits);

        model.addAttribute("donutData", Arrays.asList(costVar, costFix, costProv, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new java.util.Locale("pt", "BR"));
        String dateStr = today.format(formatter);
        String[] parts = dateStr.split(" ");
        if(parts.length > 2) {
            parts[2] = parts[2].substring(0, 1).toUpperCase() + parts[2].substring(1);
            dateStr = String.join(" ", parts);
        }
        model.addAttribute("currentDateStr", dateStr);

        return "dashboard";
    }
}