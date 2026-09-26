package com.alfatahi.erp.controller;

import com.alfatahi.erp.service.FinancialPeriod;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.QuoteRepository;
import com.alfatahi.erp.service.ScheduleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/commercial")
public class CommercialController {

    private final QuoteRepository quoteRepo;
    private final ClientRepository clientRepo;
    private final ScheduleService scheduleService;

    public CommercialController(QuoteRepository quoteRepo, ClientRepository clientRepo, ScheduleService scheduleService) {
        this.quoteRepo = quoteRepo;
        this.clientRepo = clientRepo;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) String month, Model model) {
        FinancialPeriod period = FinancialPeriod.select(month, false, null, null);
        period.addTo(model);
        List<Quote> quotes = quoteRepo.findAll();
        List<Quote> allQuotes = quotes.stream().filter(q -> period.contains(q.getDateCreated())).toList();
        List<Quote> sales = quotes.stream().filter(q -> "approved".equals(q.getStatus())
                && period.contains(q.getDateApproved())).toList();
        long totalClients = clientRepo.findAll().stream().filter(c -> period.contains(c.getCreatedAt())).count();
        long totalQuotes = allQuotes.size();

        long pending = allQuotes.stream().filter(q -> "pending".equals(q.getStatus())).count();
        long approved = sales.size();
        long expired = allQuotes.stream().filter(q -> "expired".equals(q.getStatus())).count();
        long cancelled = allQuotes.stream().filter(q -> "cancelled".equals(q.getStatus())).count();

        BigDecimal valorVendido = sales.stream()
                .filter(q -> "approved".equals(q.getStatus()))
                .map(q -> q.getTotalValue() != null ? q.getTotalValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalReceipts = approved;

        model.addAttribute("currentPage", "commercial");
        model.addAttribute("totalQuotes", totalQuotes);
        model.addAttribute("totalClients", totalClients);
        model.addAttribute("totalReceipts", totalReceipts);
        model.addAttribute("valorVendido", valorVendido);

        model.addAttribute("countPending", pending);
        model.addAttribute("countApproved", approved);
        model.addAttribute("countCancelled", cancelled);
        model.addAttribute("countExpired", expired);

        Map<String, Object> agendaKpis = scheduleService.getKpis();
        model.addAttribute("agendaKpis", agendaKpis);


        List<Quote> recentQuotes = allQuotes.stream()
                .sorted((q1, q2) -> {
                    if (q1.getDateCreated() == null && q2.getDateCreated() == null) return 0;
                    if (q1.getDateCreated() == null) return 1;
                    if (q2.getDateCreated() == null) return -1;
                    return q2.getDateCreated().compareTo(q1.getDateCreated());
                })
                .limit(6)
                .toList();
        model.addAttribute("recentQuotes", recentQuotes);

        return "commercial-dashboard";
    }
}