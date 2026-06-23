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

        long atrasados = receivables.stream().filter(r -> "pending".equals(r.getStatus()) && r.getDueDate().isBefore(LocalDate.now())).count();
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
        int currentYear = LocalDate.now().getYear();
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

        // ==========================================
        // 4. VISÃO 3: PERFORMANCE POR TIPO DE SERVIÇO
        // ==========================================
        Map<String, BigDecimal> revenueByCat = new HashMap<>();
        Map<String, BigDecimal> profitByCat = new HashMap<>();
        BigDecimal lucroBrutoTotal = BigDecimal.ZERO;

        for (WorkOrder wo : workOrders) {
            String catName = (wo.getCategory() != null && wo.getCategory().getName() != null) ? wo.getCategory().getName() : "Diversos";
            BigDecimal receita = wo.getTotalValue() != null ? wo.getTotalValue() : BigDecimal.ZERO;

            // Custo real mapeado dos materiais (Vidros, alumínios)
            BigDecimal custo = workOrderService.calculateObraCost(wo.getId());
            if(custo == null) custo = BigDecimal.ZERO;

            BigDecimal lucro = receita.subtract(custo);
            lucroBrutoTotal = lucroBrutoTotal.add(lucro);

            revenueByCat.put(catName, revenueByCat.getOrDefault(catName, BigDecimal.ZERO).add(receita));
            profitByCat.put(catName, profitByCat.getOrDefault(catName, BigDecimal.ZERO).add(lucro));
        }

        dto.setLucroBruto(lucroBrutoTotal);
        if (receitaTotalOs.compareTo(BigDecimal.ZERO) > 0) {
            dto.setMargemMedia(lucroBrutoTotal.multiply(new BigDecimal("100")).divide(receitaTotalOs, 2, RoundingMode.HALF_UP));
        }

        // Converte os Mapas para Listas que o Gráfico entenda
        List<String> catLabels = new ArrayList<>(revenueByCat.keySet());
        List<BigDecimal> catRevenues = new ArrayList<>();
        List<BigDecimal> catProfits = new ArrayList<>();
        for(String cat : catLabels) {
            catRevenues.add(revenueByCat.get(cat));
            catProfits.add(profitByCat.get(cat));
        }

        // ==========================================
        // 5. INJEÇÃO NO HTML
        // ==========================================
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("dash", dto);
        model.addAttribute("topWorkOrders", workOrders.stream().limit(5).toList());
        model.addAttribute("upcomingPayables", payables.stream().filter(p -> "pending".equals(p.getStatus())).limit(4).toList());

        // Variáveis Dinâmicas para o Chart.js
        model.addAttribute("chartMonths", "['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']");
        model.addAttribute("chartIn", Arrays.toString(monthRevenues));
        model.addAttribute("chartOut", Arrays.toString(monthExpenses));

        model.addAttribute("donutData", "[" + costVar + ", " + costFix + ", " + costProv + "]");

        model.addAttribute("barLabels", catLabels.isEmpty() ? "[]" : "['" + String.join("', '", catLabels) + "']");
        model.addAttribute("barRevenues", catRevenues.toString());
        model.addAttribute("barProfits", catProfits.toString());

        return "dashboard";
    }
}