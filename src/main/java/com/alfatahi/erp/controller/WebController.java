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

        if (mes == null) mes = hoje.getMonthValue();
        if (ano == null) ano = hoje.getYear();

        if (mes < 1 || mes > 12) mes = hoje.getMonthValue();
        if (ano < 2000 || ano > 2100) ano = hoje.getYear();

        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate inicioMes = ym.atDay(1);
        LocalDate fimMes    = ym.atEndOfMonth().plusDays(1);




        BigDecimal totalRecebidoReal = receivableRepo.findAll().stream()
                .filter(r -> ("received".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                        && r.getPaymentDate() != null)
                .map(AccountsReceivable::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPagoReal = payableRepo.findAll().stream()
                .filter(p -> ("paid".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && p.getPaymentDate() != null)
                .map(AccountsPayable::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoReal = totalRecebidoReal.subtract(totalPagoReal);

        BigDecimal entradasHoje = BigDecimal.ZERO;
        BigDecimal saidasHoje = BigDecimal.ZERO;
        if (mes == hoje.getMonthValue() && ano == hoje.getYear()) {
            entradasHoje = receivableRepo.sumEntradasRealByPeriod(hoje, hoje.plusDays(1));
            if (entradasHoje == null) entradasHoje = BigDecimal.ZERO;

            saidasHoje = payableRepo.sumSaidasRealByPeriod(hoje, hoje.plusDays(1));
            if (saidasHoje == null) saidasHoje = BigDecimal.ZERO;
        }

        BigDecimal taxasCartaoMes = nvl(receivableRepo.sumTaxasCartaoByPeriod(inicioMes, fimMes));




        BigDecimal totalAReceber = receivableRepo.findAll().stream()
                .filter(r -> ("pending".equals(r.getStatus()) || "partial".equals(r.getStatus()))
                        && r.getDueDate() != null
                        && !r.getDueDate().isBefore(inicioMes)
                        && r.getDueDate().isBefore(fimMes))
                .map(r -> r.getBalance() != null ? r.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAPagar = payableRepo.findAll().stream()
                .filter(p -> ("pending".equals(p.getStatus()) || "partial".equals(p.getStatus()))
                        && p.getDueDate() != null
                        && !p.getDueDate().isBefore(inicioMes)
                        && p.getDueDate().isBefore(fimMes))
                .map(p -> p.getBalance() != null ? p.getBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal entradasPrevistasMes = totalAReceber;

        BigDecimal receitasAntecipadas = nvl(receivableRepo.sumReceitasAntecipadas(inicioMes, fimMes));

        BigDecimal receitasFuturas = nvl(receivableRepo.sumReceitasFuturas(inicioMes, fimMes));

        BigDecimal saldoProjetado = saldoReal.add(totalAReceber).subtract(totalAPagar);

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




        BigDecimal receitaBrutaMes = nvl(receivableRepo.sumReceivedByMonthAndYear(inicioMes, fimMes));
        
        BigDecimal totalDespesasMes = nvl(payableRepo.sumSaidasRealByPeriod(inicioMes, fimMes));

        BigDecimal totalPerdas = nvl(lossRepo.sumFinancialImpactByPeriod(inicioMes, fimMes));

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

        BigDecimal receitaOsMes = nvl(workOrderRepo.sumRevenueConcludedByPeriod(inicioMes, fimMes));

        BigDecimal ticketMedioOsMes = BigDecimal.ZERO;
        if (osConcluidasMes > 0) {
            ticketMedioOsMes = receitaOsMes.divide(BigDecimal.valueOf(osConcluidasMes), 2, RoundingMode.HALF_UP);
        }

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



        BigDecimal fixedTotalMes = nvl(payableRepo.sumFixedTotalByMonth(inicioMes, fimMes));
        BigDecimal fixedPagoMes = nvl(payableRepo.sumFixedPaidByMonth(inicioMes, fimMes));
        BigDecimal fixedPendenteMes = nvl(payableRepo.sumFixedPendingByMonth(inicioMes, fimMes));
        List<AccountsPayable> proximasContasFixas = payableRepo.findUpcomingFixedDue(hoje).stream()
                .limit(5)
                .collect(Collectors.toList());

        String nomeMes = java.time.Month.of(mes).toString();

        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);
        model.addAttribute("nomeMes", nomeMes);
        model.addAttribute("periodoExibicao", String.format("%s/%d", mes < 10 ? "0" + mes : mes, ano));

        model.addAttribute("saldoAtual",        saldoReal);
        model.addAttribute("entradasHoje",       entradasHoje);
        model.addAttribute("saidasHoje",         saidasHoje);
        model.addAttribute("taxasCartaoMes",     taxasCartaoMes);

        model.addAttribute("totalAReceber",      totalAReceber);
        model.addAttribute("totalAPagar",        totalAPagar);
        model.addAttribute("entradasPrevistasMes", entradasPrevistasMes);
        model.addAttribute("receitasAntecipadas",  receitasAntecipadas);
        model.addAttribute("receitasFuturas",      receitasFuturas);
        model.addAttribute("saldoProjetado",     saldoProjetado);

        model.addAttribute("receitaBrutaMes",   receitaBrutaMes);
        model.addAttribute("totalDespesasMes",  totalDespesasMes);
        model.addAttribute("aReceberAtrasado",  aReceberAtrasado);
        model.addAttribute("aPagarVencido",     aPagarVencido);
        model.addAttribute("totalPerdas",       totalPerdas);
        model.addAttribute("margemPerdas",      margemPerdas);

        model.addAttribute("totalReceitas",  receitaBrutaMes);
        model.addAttribute("totalDespesas",  totalDespesasMes);
        model.addAttribute("barReceitas",    barReceitas);
        model.addAttribute("barDespesas",    barDespesas);
        model.addAttribute("barPerdas",      barPerdas);

        model.addAttribute("osEmAndamento",  osEmAndamento);
        model.addAttribute("osConcluidasMes",osConcluidasMes);
        model.addAttribute("osAbertas",      osAbertas);
        model.addAttribute("osEmProducao",   osEmProducao);
        model.addAttribute("osProntas",      osProntas);
        model.addAttribute("osAtrasadas",    osAtrasadas);
        model.addAttribute("osConcluidas",   osConcluidas);
        model.addAttribute("ultimasOs",      ultimasOs);

        model.addAttribute("orcamentosPendentes", orcamentosPendentes);
        model.addAttribute("orcamentosAprovados", orcamentosAprovados);
        model.addAttribute("orcamentosValor",     orcamentosValor);
        model.addAttribute("taxaConversao",       taxaConversao);

        model.addAttribute("agendaHoje", agendaHoje);

        model.addAttribute("fixedTotalMes", fixedTotalMes);
        model.addAttribute("fixedPagoMes", fixedPagoMes);
        model.addAttribute("fixedPendenteMes", fixedPendenteMes);
        model.addAttribute("proximasContasFixas", proximasContasFixas);

        return "dashboard";
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}