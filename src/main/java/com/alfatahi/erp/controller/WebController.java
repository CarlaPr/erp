package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.Schedule;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.service.CashLedgerService;
import com.alfatahi.erp.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final AccountsReceivableRepository receivableRepo;
    private final AccountsPayableRepository payableRepo;
    private final WorkOrderRepository workOrderRepo;
    private final QuoteRepository quoteRepo;
    private final LossRepository lossRepo;
    private final ScheduleService scheduleService;
    private final CashLedgerService cashLedgerService;

    public WebController(
            AccountsReceivableRepository receivableRepo,
            AccountsPayableRepository payableRepo,
            WorkOrderRepository workOrderRepo,
            QuoteRepository quoteRepo,
            LossRepository lossRepo,
            ScheduleService scheduleService,
            CashLedgerService cashLedgerService) {
        this.receivableRepo   = receivableRepo;
        this.payableRepo      = payableRepo;
        this.workOrderRepo    = workOrderRepo;
        this.quoteRepo        = quoteRepo;
        this.lossRepo         = lossRepo;
        this.scheduleService  = scheduleService;
        this.cashLedgerService = cashLedgerService;
    }

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(name = "mes", required = false) Integer mes,
            @RequestParam(name = "ano", required = false) Integer ano,
            Model model) {
        LocalDate hoje      = LocalDate.now();

        // Se não foi selecionado mês/ano, usa o atual
        if (mes == null) mes = hoje.getMonthValue();
        if (ano == null) ano = hoje.getYear();

        // Validar valores
        if (mes < 1 || mes > 12) mes = hoje.getMonthValue();
        if (ano < 2000 || ano > 2100) ano = hoje.getYear();

        // Calcula período do mês selecionado
        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate fimMes    = ym.atEndOfMonth().plusDays(1);  // exclusive para queries

        // ═══════════════════════════════════════════════════════════════════════
        // CAIXA REAL — Global (Independente do mês selecionado)
        // ═══════════════════════════════════════════════════════════════════════

        // Total recebido de todos os tempos (saldo real global)
        BigDecimal totalRecebidoReal = receivableRepo.findAll().stream()
                .filter(r -> ("received".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                        && r.getPaymentDate() != null)
                .map(AccountsReceivable::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total pago de todos os tempos (saldo real global)
        BigDecimal totalPagoReal = payableRepo.findAll().stream()
                .filter(p -> ("paid".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && p.getPaymentDate() != null)
                .map(AccountsPayable::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Saldo Real = Todo dinheiro que já entrou – todo dinheiro que já saiu (Saldo Global)
        BigDecimal saldoReal = totalRecebidoReal.subtract(totalPagoReal);

        // Entradas de hoje (se for o mês atual)
        BigDecimal entradasHoje = BigDecimal.ZERO;
        BigDecimal saidasHoje = BigDecimal.ZERO;
        if (mes == hoje.getMonthValue() && ano == hoje.getYear()) {
            entradasHoje = receivableRepo.sumEntradasRealByPeriod(hoje, hoje.plusDays(1));
            if (entradasHoje == null) entradasHoje = BigDecimal.ZERO;

            saidasHoje = payableRepo.sumSaidasRealByPeriod(hoje, hoje.plusDays(1));
            if (saidasHoje == null) saidasHoje = BigDecimal.ZERO;
        }

        // Taxas de cartão do mês selecionado
        BigDecimal taxasCartaoMes = nvl(receivableRepo.sumTaxasCartaoByPeriod(inicioMes, fimMes));

        // ═══════════════════════════════════════════════════════════════════════
        // CAIXA PREVISTO — valores que ainda vão entrar ou sair no mês selecionado
        // ═══════════════════════════════════════════════════════════════════════

        // Contas a receber do mês: pendentes ou parcialmente pagas com vencimento no mês
        BigDecimal totalAReceber = receivableRepo.findAll().stream()
                .filter(r -> ("pending".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                        && r.getDueDate() != null
                        && !r.getDueDate().isBefore(inicioMes)
                        && r.getDueDate().isBefore(fimMes))
                .map(r -> r.getBalance() != null ? r.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Contas a pagar do mês: pendentes ou parcialmente pagas com vencimento no mês
        BigDecimal totalAPagar = payableRepo.findAll().stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && p.getDueDate() != null
                        && !p.getDueDate().isBefore(inicioMes)
                        && p.getDueDate().isBefore(fimMes))
                .map(p -> p.getBalance() != null ? p.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Entradas previstas no mês: pendentes com vencimento no mês
        BigDecimal entradasPrevistasMes = totalAReceber;

        // Receitas antecipadas: pagas hoje mas competência futura
        BigDecimal receitasAntecipadas = nvl(receivableRepo.sumReceitasAntecipadas(inicioMes, fimMes));

        // Receitas futuras: pendentes com competência no mês
        BigDecimal receitasFuturas = nvl(receivableRepo.sumReceitasFuturas(inicioMes, fimMes));

        // Saldo projetado = Saldo Real + A Receber - A Pagar (projeção, não fato)
        BigDecimal saldoProjetado = saldoReal.add(totalAReceber).subtract(totalAPagar);

        // Recebimentos em atraso até o final do mês selecionado
        BigDecimal aReceberAtrasado = receivableRepo.findAll().stream()
                .filter(r -> ("pending".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                        && r.getDueDate() != null
                        && r.getDueDate().isBefore(fimMes))
                .map(r -> r.getBalance() != null ? r.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal aPagarVencido = payableRepo.findAll().stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && p.getDueDate() != null
                        && p.getDueDate().isBefore(fimMes))
                .map(p -> p.getBalance() != null ? p.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ═══════════════════════════════════════════════════════════════════════
        // RECEITA DO MÊS PARA O GRÁFICO DE BARRAS
        // ═══════════════════════════════════════════════════════════════════════

        // Receita bruta recebida no mês (para DRE / barra)
        BigDecimal receitaBrutaMes = nvl(receivableRepo.sumReceivedByMonthAndYear(inicioMes, fimMes));

        // Despesas totais do mês (pagas)
        BigDecimal totalDespesasMes = nvl(payableRepo.sumSaidasRealByPeriod(inicioMes, fimMes));

        BigDecimal totalPerdas = lossRepo.findAll().stream()
                .map(l -> l.getFinancialImpact() != null ? l.getFinancialImpact() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxBar = receitaBrutaMes.max(totalDespesasMes).max(totalPerdas);
        int barReceitas = 0, barDespesas = 0, barPerdas = 0;
        if (maxBar.compareTo(BigDecimal.ZERO) > 0) {
            barReceitas = receitaBrutaMes.multiply(BigDecimal.valueOf(100)).divide(maxBar, 0, RoundingMode.HALF_UP).intValue();
            barDespesas = totalDespesasMes.multiply(BigDecimal.valueOf(100)).divide(maxBar, 0, RoundingMode.HALF_UP).intValue();
            barPerdas   = totalPerdas.multiply(BigDecimal.valueOf(100)).divide(maxBar, 0, RoundingMode.HALF_UP).intValue();
        }

        BigDecimal margemPerdas = BigDecimal.ZERO;
        if (receitaBrutaMes.compareTo(BigDecimal.ZERO) > 0) {
            margemPerdas = totalPerdas
                    .multiply(BigDecimal.valueOf(100))
                    .divide(receitaBrutaMes, 2, RoundingMode.HALF_UP);
        }

        // ═══════════════════════════════════════════════════════════════════════
        // ORDENS DE SERVIÇO (filtradas pelo mês selecionado)
        // ═══════════════════════════════════════════════════════════════════════

        long osEmAndamento = workOrderRepo.findAll().stream()
                .filter(wo -> ("in_progress".equalsIgnoreCase(wo.getStatus())
                        || "pending".equalsIgnoreCase(wo.getStatus())
                        || "aberta".equalsIgnoreCase(wo.getStatus())
                        || "em_producao".equalsIgnoreCase(wo.getStatus()))
                        && wo.getCreatedAt() != null
                        && !wo.getCreatedAt().toLocalDate().isBefore(inicioMes)
                        && wo.getCreatedAt().toLocalDate().isBefore(fimMes))
                .count();

        long osConcluidasMes = workOrderRepo.findAll().stream()
                .filter(wo -> ("completed".equalsIgnoreCase(wo.getStatus())
                        || "done".equalsIgnoreCase(wo.getStatus())
                        || "concluida".equalsIgnoreCase(wo.getStatus()))
                        && wo.getInstallDate() != null
                        && !wo.getInstallDate().isBefore(inicioMes)
                        && wo.getInstallDate().isBefore(fimMes))
                .count();

        long osAbertas = workOrderRepo.findAll().stream()
                .filter(w -> ("aberta".equalsIgnoreCase(w.getStatus()) || "em_producao".equalsIgnoreCase(w.getStatus()))
                        && w.getCreatedAt() != null
                        && !w.getCreatedAt().toLocalDate().isBefore(inicioMes)
                        && w.getCreatedAt().toLocalDate().isBefore(fimMes))
                .count();

        long osEmProducao = workOrderRepo.findAll().stream()
                .filter(w -> "em_producao".equalsIgnoreCase(w.getStatus())
                        && w.getCreatedAt() != null
                        && !w.getCreatedAt().toLocalDate().isBefore(inicioMes)
                        && w.getCreatedAt().toLocalDate().isBefore(fimMes))
                .count();

        long osProntas = workOrderRepo.findAll().stream()
                .filter(w -> "pronta".equalsIgnoreCase(w.getStatus())
                        && w.getCreatedAt() != null
                        && !w.getCreatedAt().toLocalDate().isBefore(inicioMes)
                        && w.getCreatedAt().toLocalDate().isBefore(fimMes))
                .count();

        long osAtrasadas = workOrderRepo.findAll().stream()
                .filter(w -> !"concluida".equalsIgnoreCase(w.getStatus())
                        && !"cancelada".equalsIgnoreCase(w.getStatus())
                        && w.getInstallDate() != null
                        && w.getInstallDate().isBefore(fimMes)
                        && w.getCreatedAt() != null
                        && !w.getCreatedAt().toLocalDate().isBefore(inicioMes))
                .count();

        long osConcluidas = workOrderRepo.findAll().stream()
                .filter(w -> "concluida".equalsIgnoreCase(w.getStatus())
                        && w.getInstallDate() != null
                        && !w.getInstallDate().isBefore(inicioMes)
                        && w.getInstallDate().isBefore(fimMes))
                .count();

        List<?> ultimasOs = workOrderRepo.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .collect(Collectors.toList());

        // ═══════════════════════════════════════════════════════════════════════
        // ORÇAMENTOS (filtrados pelo mês selecionado)
        // ═══════════════════════════════════════════════════════════════════════

        long orcamentosPendentes = quoteRepo.findAll().stream()
                .filter(q -> "pending".equalsIgnoreCase(q.getStatus())
                        && q.getDateCreated() != null
                        && !q.getDateCreated().toLocalDate().isBefore(inicioMes)
                        && q.getDateCreated().toLocalDate().isBefore(fimMes))
                .count();

        long orcamentosAprovados = quoteRepo.findAll().stream()
                .filter(q -> "approved".equalsIgnoreCase(q.getStatus())
                        && q.getDateApproved() != null
                        && !q.getDateApproved().toLocalDate().isBefore(inicioMes)
                        && q.getDateApproved().toLocalDate().isBefore(fimMes))
                .count();

        BigDecimal orcamentosValor = quoteRepo.findAll().stream()
                .filter(q -> "pending".equalsIgnoreCase(q.getStatus())
                        && q.getDateCreated() != null
                        && !q.getDateCreated().toLocalDate().isBefore(inicioMes)
                        && q.getDateCreated().toLocalDate().isBefore(fimMes))
                .map(q -> q.getTotalValue() != null ? q.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrcMes = quoteRepo.findAll().stream()
                .filter(q -> q.getDateApproved() != null
                        && !q.getDateApproved().toLocalDate().isBefore(inicioMes)
                        && q.getDateApproved().toLocalDate().isBefore(fimMes))
                .count();
        BigDecimal taxaConversao = BigDecimal.ZERO;
        if (totalOrcMes > 0) {
            taxaConversao = BigDecimal.valueOf(orcamentosAprovados * 100L)
                    .divide(BigDecimal.valueOf(totalOrcMes), 1, RoundingMode.HALF_UP);
        }

        List<Schedule> agendaHoje = scheduleService.findByDate(hoje);

        // Adicionar informações do mês selecionado
        String nomeMes = java.time.Month.of(mes).toString();

        // ── Monta o model ──────────────────────────────────────────────────────
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);
        model.addAttribute("nomeMes", nomeMes);
        model.addAttribute("periodoExibicao", String.format("%s/%d", mes < 10 ? "0" + mes : mes, ano));

        // Caixa REAL
        model.addAttribute("saldoAtual",        saldoReal);        // dinheiro disponível de fato
        model.addAttribute("entradasHoje",       entradasHoje);
        model.addAttribute("saidasHoje",         saidasHoje);
        model.addAttribute("taxasCartaoMes",     taxasCartaoMes);

        // Caixa PREVISTO (nunca misturar com saldoAtual)
        model.addAttribute("totalAReceber",      totalAReceber);
        model.addAttribute("totalAPagar",        totalAPagar);
        model.addAttribute("entradasPrevistasMes", entradasPrevistasMes);
        model.addAttribute("receitasAntecipadas",  receitasAntecipadas);
        model.addAttribute("receitasFuturas",      receitasFuturas);
        model.addAttribute("saldoProjetado",     saldoProjetado);

        // Mês corrente
        model.addAttribute("receitaBrutaMes",   receitaBrutaMes);
        model.addAttribute("totalDespesasMes",  totalDespesasMes);
        model.addAttribute("aReceberAtrasado",  aReceberAtrasado);
        model.addAttribute("aPagarVencido",     aPagarVencido);
        model.addAttribute("totalPerdas",       totalPerdas);
        model.addAttribute("margemPerdas",      margemPerdas);

        // Gráfico de barras
        model.addAttribute("totalReceitas",  receitaBrutaMes);
        model.addAttribute("totalDespesas",  totalDespesasMes);
        model.addAttribute("barReceitas",    barReceitas);
        model.addAttribute("barDespesas",    barDespesas);
        model.addAttribute("barPerdas",      barPerdas);

        // OS
        model.addAttribute("osEmAndamento",  osEmAndamento);
        model.addAttribute("osConcluidasMes",osConcluidasMes);
        model.addAttribute("osAbertas",      osAbertas);
        model.addAttribute("osEmProducao",   osEmProducao);
        model.addAttribute("osProntas",      osProntas);
        model.addAttribute("osAtrasadas",    osAtrasadas);
        model.addAttribute("osConcluidas",   osConcluidas);
        model.addAttribute("ultimasOs",      ultimasOs);

        // Orçamentos
        model.addAttribute("orcamentosPendentes", orcamentosPendentes);
        model.addAttribute("orcamentosAprovados", orcamentosAprovados);
        model.addAttribute("orcamentosValor",     orcamentosValor);
        model.addAttribute("taxaConversao",       taxaConversao);

        model.addAttribute("agendaHoje", agendaHoje);

        return "dashboard";
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}