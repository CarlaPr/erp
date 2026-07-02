package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Schedule;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class WebController {

    private final AccountsReceivableRepository receivableRepo;
    private final AccountsPayableRepository payableRepo;
    private final WorkOrderRepository workOrderRepo;
    private final QuoteRepository quoteRepo;
    private final LossRepository lossRepo;
    private final ScheduleService scheduleService;

    public WebController(
            AccountsReceivableRepository receivableRepo,
            AccountsPayableRepository payableRepo,
            WorkOrderRepository workOrderRepo,
            QuoteRepository quoteRepo,
            LossRepository lossRepo,
            ScheduleService scheduleService) {
        this.receivableRepo  = receivableRepo;
        this.payableRepo     = payableRepo;
        this.workOrderRepo   = workOrderRepo;
        this.quoteRepo       = quoteRepo;
        this.lossRepo        = lossRepo;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate hoje      = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes    = hoje.withDayOfMonth(hoje.lengthOfMonth());

        // ── RECEITAS ────────────────────────────────────────────────
        BigDecimal totalReceitas = receivableRepo.findAllByOrderByDueDateAsc().stream()
                .filter(r -> !"cancelled".equals(r.getStatus()))
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Faturamento do mês corrente (totalAmount das receitas com vencimento no mês)
        BigDecimal faturamentoMes = receivableRepo.findAllByOrderByDueDateAsc().stream()
                .filter(r -> !"cancelled".equals(r.getStatus()))
                .filter(r -> r.getDueDate() != null
                        && !r.getDueDate().isBefore(inicioMes)
                        && !r.getDueDate().isAfter(fimMes))
                .map(r -> r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total a receber (pendente/parcial)
        BigDecimal aReceberTotal = receivableRepo.findAllByOrderByDueDateAsc().stream()
                .filter(r -> "pending".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                .map(r -> r.getBalance() != null ? r.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // A receber ATRASADO
        BigDecimal aReceberAtrasado = receivableRepo.findAllByOrderByDueDateAsc().stream()
                .filter(r -> ("pending".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                        && r.getDueDate() != null && r.getDueDate().isBefore(hoje))
                .map(r -> r.getBalance() != null ? r.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── DESPESAS ────────────────────────────────────────────────
        BigDecimal totalDespesas = payableRepo.findAll().stream()
                .filter(p -> !"cancelled".equals(p.getStatus()))
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total a pagar (pendente)
        BigDecimal aPagarTotal = payableRepo.findAll().stream()
                .filter(p -> "pending".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // A pagar VENCIDO
        BigDecimal aPagarVencido = payableRepo.findAll().stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && p.getDueDate() != null && p.getDueDate().isBefore(hoje))
                .map(p -> p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── PERDAS OPERACIONAIS ──────────────────────────────────────
        BigDecimal totalPerdas = lossRepo.findAll().stream()
                .map(l -> l.getFinancialImpact() != null ? l.getFinancialImpact() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Margem de perdas (%) sobre faturamento total
        BigDecimal margemPerdas = BigDecimal.ZERO;
        if (totalReceitas.compareTo(BigDecimal.ZERO) > 0) {
            margemPerdas = totalPerdas
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalReceitas, 2, RoundingMode.HALF_UP);
        }

        // ── RESULTADO LÍQUIDO & SALDO PROJETADO ─────────────────────
        BigDecimal resultadoLiquido = totalReceitas.subtract(totalDespesas);
        BigDecimal saldoProjetado   = totalReceitas.subtract(totalDespesas).subtract(totalPerdas);

        // ── BARRAS PROPORCIONAIS (max 100%) ─────────────────────────
        BigDecimal maxBar = totalReceitas.max(totalDespesas).max(totalPerdas);
        int barReceitas = 0, barDespesas = 0, barPerdas = 0;
        if (maxBar.compareTo(BigDecimal.ZERO) > 0) {
            barReceitas = totalReceitas.multiply(BigDecimal.valueOf(100)).divide(maxBar, 0, RoundingMode.HALF_UP).intValue();
            barDespesas = totalDespesas.multiply(BigDecimal.valueOf(100)).divide(maxBar, 0, RoundingMode.HALF_UP).intValue();
            barPerdas   = totalPerdas.multiply(BigDecimal.valueOf(100)).divide(maxBar, 0, RoundingMode.HALF_UP).intValue();
        }

        // ── ORDENS DE SERVIÇO ────────────────────────────────────────
        long osAbertas    = workOrderRepo.findAll().stream().filter(w -> "aberta".equalsIgnoreCase(w.getStatus()) || "em_producao".equalsIgnoreCase(w.getStatus())).count();
        long osEmProducao = workOrderRepo.findAll().stream().filter(w -> "em_producao".equalsIgnoreCase(w.getStatus())).count();
        long osProntas    = workOrderRepo.findAll().stream().filter(w -> "pronta".equalsIgnoreCase(w.getStatus())).count();
        long osAtrasadas  = workOrderRepo.findAll().stream()
                .filter(w -> !"concluida".equalsIgnoreCase(w.getStatus()) && !"cancelada".equalsIgnoreCase(w.getStatus())
                        && w.getInstallDate() != null && w.getInstallDate().isBefore(hoje))
                .count();
        long osConcluidas = workOrderRepo.findAll().stream()
                .filter(w -> "concluida".equalsIgnoreCase(w.getStatus())
                        && w.getInstallDate() != null
                        && !w.getInstallDate().isBefore(inicioMes)
                        && !w.getInstallDate().isAfter(fimMes))
                .count();

        // Últimas 5 OS
        List<?> ultimasOs = workOrderRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .collect(Collectors.toList());

        // ── ORÇAMENTOS ───────────────────────────────────────────────
        long orcamentosPendentes = quoteRepo.findAll().stream().filter(q -> "pending".equalsIgnoreCase(q.getStatus())).count();
        long orcamentosAprovados = quoteRepo.findAll().stream()
                .filter(q -> "approved".equalsIgnoreCase(q.getStatus())
                        && q.getDateApproved() != null
                        && !q.getDateApproved().toLocalDate().isBefore(inicioMes)
                        && !q.getDateApproved().toLocalDate().isAfter(fimMes))
                .count();
        BigDecimal orcamentosValor = quoteRepo.findAll().stream()
                .filter(q -> "pending".equalsIgnoreCase(q.getStatus()))
                .map(q -> q.getTotalValue() != null ? q.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Taxa de aprovação (mês)
        long totalOrcMes = quoteRepo.findAll().stream()
                .filter(q -> q.getDateApproved() != null
                        && !q.getDateApproved().toLocalDate().isBefore(inicioMes)
                        && !q.getDateApproved().toLocalDate().isAfter(fimMes))
                .count();
        BigDecimal taxaConversao = BigDecimal.ZERO;
        if (totalOrcMes > 0) {
            taxaConversao = BigDecimal.valueOf(orcamentosAprovados * 100L)
                    .divide(BigDecimal.valueOf(totalOrcMes), 1, RoundingMode.HALF_UP);
        }

        // ── AGENDA DO DIA ────────────────────────────────────────────
        List<Schedule> agendaHoje = scheduleService.findByDate(hoje);

        // ── BIND AO MODEL ────────────────────────────────────────────
        model.addAttribute("currentPage",       "dashboard");

        // KPI strip
        model.addAttribute("faturamentoMes",    faturamentoMes);
        model.addAttribute("aReceberTotal",     aReceberTotal);
        model.addAttribute("aPagarTotal",       aPagarTotal);
        model.addAttribute("resultadoLiquido",  resultadoLiquido);
        model.addAttribute("osAbertas",         osAbertas);

        // Fluxo de Caixa
        model.addAttribute("totalReceitas",     totalReceitas);
        model.addAttribute("totalDespesas",     totalDespesas);
        model.addAttribute("totalPerdas",       totalPerdas);
        model.addAttribute("saldoProjetado",    saldoProjetado);
        model.addAttribute("barReceitas",       barReceitas);
        model.addAttribute("barDespesas",       barDespesas);
        model.addAttribute("barPerdas",         barPerdas);

        // Status OS
        model.addAttribute("osEmProducao",      osEmProducao);
        model.addAttribute("osProntas",         osProntas);
        model.addAttribute("osAtrasadas",       osAtrasadas);
        model.addAttribute("osConcluidas",      osConcluidas);

        // Alertas financeiros
        model.addAttribute("aReceberAtrasado",  aReceberAtrasado);
        model.addAttribute("aPagarVencido",     aPagarVencido);
        model.addAttribute("margemPerdas",      margemPerdas);

        // Orçamentos
        model.addAttribute("orcamentosPendentes", orcamentosPendentes);
        model.addAttribute("orcamentosAprovados", orcamentosAprovados);
        model.addAttribute("orcamentosValor",     orcamentosValor);
        model.addAttribute("taxaConversao",       taxaConversao);

        // Agenda + tabela OS
        model.addAttribute("agendaHoje",        agendaHoje);
        model.addAttribute("ultimasOs",         ultimasOs);

        return "dashboard";
    }
}