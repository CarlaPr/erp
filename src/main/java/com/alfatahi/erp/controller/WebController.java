package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.DashboardDto;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.Loss;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.LossRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private final AccountsPayableRepository payableRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final WorkOrderService workOrderService;
    private final LossRepository lossRepository;
    private final WorkOrderRepository workOrderRepository;

    public WebController(AccountsPayableRepository payableRepository,
                         AccountsReceivableRepository receivableRepository,
                         WorkOrderService workOrderService, LossRepository lossRepository, WorkOrderRepository workOrderRepository) {
        this.payableRepository = payableRepository;
        this.receivableRepository = receivableRepository;
        this.workOrderService = workOrderService;
        this.lossRepository = lossRepository;
        this.workOrderRepository = workOrderRepository;
    }

    @Transactional(readOnly = true)
    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @Transactional(readOnly = true)
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        DashboardDto dto = new DashboardDto();
        List<AccountsReceivable> receivables =
                receivableRepository.findByStatusNotOrderByDueDateAsc("cancelled");
        List<AccountsPayable> payables = payableRepository.findAllByOrderByDueDateAsc();
        List<WorkOrder> workOrders = workOrderRepository.findAllWithItemsOrderByCreatedAtDesc();
        LocalDate today = LocalDate.now();

        // ==========================================
        // 1. CÁLCULOS DOS CARDS (KPIs REAIS COM PROTEÇÃO NULL)
        // ==========================================
        BigDecimal faturado = receivables.stream()
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal recebido = receivables.stream()
                // Valor líquido = bruto gravado - taxa de maquininha acumulada
                .map(r -> r.getNetReceivedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setaReceber(faturado.subtract(recebido));
        dto.setRecebido(recebido);

        BigDecimal cadastrado = payables.stream()
                .filter(p -> !"cancelled".equals(p.getStatus()))
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pago = payables.stream()
                .filter(p -> "paid".equals(p.getStatus()))
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dto.setaPagar(cadastrado.subtract(pago));
        dto.setPago(pago);

        dto.setSaldoAtual(recebido.subtract(pago));
        dto.setSaldoProjetado(dto.getSaldoAtual().add(dto.getaReceber()).subtract(dto.getaPagar()));

        long atrasados = receivables.stream()
                .filter(r -> "pending".equals(r.getStatus()) && r.getDueDate() != null && r.getDueDate().isBefore(today))
                .count();
        dto.setInadimplentes(atrasados);

        long osAtivas = workOrders.stream()
                .filter(wo -> !"completed".equals(wo.getStatus()) && !"cancelled".equals(wo.getStatus()))
                .count();
        dto.setOsAtivas(osAtivas);

        BigDecimal receitaTotalOs = workOrders.stream()
                .map(wo -> wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!workOrders.isEmpty()) {
            dto.setTicketMedio(receitaTotalOs.divide(new BigDecimal(workOrders.size()), 2, RoundingMode.HALF_UP));
        }

        // Inject LossRepository via construtor
        BigDecimal totalPerdas = lossRepository.findAll().stream()
                .map(Loss::getFinancialImpact)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setPerdas(totalPerdas);

        if (faturado.compareTo(BigDecimal.ZERO) > 0) {
            dto.setPerdasPercentual(
                    totalPerdas.multiply(new BigDecimal("100"))
                            .divide(faturado, 2, RoundingMode.HALF_UP)
            );
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
        // 3. VISÃO 2: CUSTOS POR SUBCATEGORIA
        // ==========================================
        Map<String, BigDecimal> expensesBySubcategory = new HashMap<>();
        BigDecimal totalCosts = BigDecimal.ZERO;

        for (AccountsPayable p : payables) {
            BigDecimal val = p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO;
            totalCosts = totalCosts.add(val);

            String sub = p.getSubcategory();
            if (sub != null && !sub.trim().isEmpty()) {
                String normalizedSub = sub.toLowerCase().trim();
                expensesBySubcategory.put(
                        normalizedSub,
                        expensesBySubcategory.getOrDefault(normalizedSub, BigDecimal.ZERO).add(val)
                );
            } else {
                expensesBySubcategory.put(
                        "outros",
                        expensesBySubcategory.getOrDefault("outros", BigDecimal.ZERO).add(val)
                );
            }
        }

        model.addAttribute("expensesBySubcategory", expensesBySubcategory);
        model.addAttribute("totalCosts", totalCosts);

        // ==========================================
        // 4. VISÃO 3: PERFORMANCE POR TIPO DE SERVIÇO
        // ==========================================
        Map<String, BigDecimal> revenueByCat = new HashMap<>();
        Map<String, BigDecimal> profitByCat = new HashMap<>();
        BigDecimal lucroBrutoTotalGeral = BigDecimal.ZERO;

        for (WorkOrder wo : workOrders) {
            BigDecimal receita = wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO;
            BigDecimal custo = wo.getTotalCost();
            if (custo == null) custo = BigDecimal.ZERO;
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
                .filter(r -> "pending".equals(r.getStatus()))
                .collect(Collectors.toList());

        List<AccountsPayable> allPendingPayables = payables.stream()
                .filter(p -> "pending".equals(p.getStatus()))
                .collect(Collectors.toList());

        List<AccountsReceivable> overdueReceivables = allPendingReceivables.stream()
                .filter(r -> r.getDueDate() != null && r.getDueDate().isBefore(today))
                .collect(Collectors.toList());

        List<AccountsPayable> overduePayables = allPendingPayables.stream()
                .filter(p -> p.getDueDate() != null && p.getDueDate().isBefore(today))
                .collect(Collectors.toList());

        List<WorkOrder> expiringWorkOrders = workOrders.stream()
                .filter(wo -> !"completed".equals(wo.getStatus()) && !"cancelled".equals(wo.getStatus()))
                .filter(wo -> wo.getInstallDate() != null && !wo.getInstallDate().isAfter(today.plusDays(3)))
                .collect(Collectors.toList());

        // ==========================================
        // 6. CALENDÁRIO UNIFICADO (ENTRADAS E SAÍDAS)
        // ==========================================
        List<Map<String, Object>> calendarEvents = new ArrayList<>();

        for (AccountsReceivable r : allPendingReceivables) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("type", "IN");
            String clientName = (r.getClient() != null) ? r.getClient().getName() : "Avulso";
            String desc = (r.getDescription() != null) ? r.getDescription() : "Recebimento";
            ev.put("description", desc + " - " + clientName);
            ev.put("date", r.getDueDate());
            ev.put("amount", r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO);
            calendarEvents.add(ev);
        }

        for (AccountsPayable p : allPendingPayables) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("type", "OUT");
            String desc = p.getDescription() != null ? p.getDescription() : "Despesa";
            String sub = p.getSubcategory() != null ? " (" + p.getSubcategory() + ")" : "";
            ev.put("description", desc + sub);
            ev.put("subcategory", p.getSubcategory() != null ? p.getSubcategory().toLowerCase().trim() : "outros");
            ev.put("date", p.getDueDate());
            ev.put("amount", p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO);
            calendarEvents.add(ev);
        }

        calendarEvents.sort((a, b) -> {
            LocalDate d1 = (LocalDate) a.get("date");
            LocalDate d2 = (LocalDate) b.get("date");
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d1.compareTo(d2);
        });

        // ==========================================
        // 7. INJEÇÃO NO HTML (MODEL)
        // ==========================================
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("dash", dto);
        model.addAttribute("allPendingReceivables", allPendingReceivables);
        model.addAttribute("allPendingPayables", allPendingPayables);
        model.addAttribute("overdueReceivables", overdueReceivables);
        model.addAttribute("overduePayables", overduePayables);
        model.addAttribute("expiringWorkOrders", expiringWorkOrders);
        model.addAttribute("calendarEvents", calendarEvents.stream().limit(16).collect(Collectors.toList()));
        model.addAttribute("monthlyFlow", monthlyFlow);
        model.addAttribute("barLabels", catLabels);
        model.addAttribute("barRevenues", catRevenues);
        model.addAttribute("barProfits", catProfits);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
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