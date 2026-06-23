package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.QuoteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/commercial")
public class CommercialController {

    private final QuoteRepository quoteRepo;
    private final ClientRepository clientRepo;

    public CommercialController(QuoteRepository quoteRepo, ClientRepository clientRepo) {
        this.quoteRepo = quoteRepo;
        this.clientRepo = clientRepo;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<Quote> allQuotes = quoteRepo.findAll();
        long totalClients = clientRepo.count();
        long totalQuotes = allQuotes.size();

        // Contadores dinâmicos por status
        long pending = allQuotes.stream().filter(q -> "pending".equals(q.getStatus())).count();
        long approved = allQuotes.stream().filter(q -> "approved".equals(q.getStatus())).count();
        long cancelled = allQuotes.stream().filter(q -> "cancelled".equals(q.getStatus())).count();
        long expired = allQuotes.stream().filter(q -> "expired".equals(q.getStatus())).count();

        // Soma do valor total vendido (apenas orçamentos aprovados)
        BigDecimal valorVendido = allQuotes.stream()
                .filter(q -> "approved".equals(q.getStatus()))
                .map(Quote::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total de recibos comerciais emitidos (atrelados a vendas aprovadas)
        long totalReceipts = approved;

        // Injeção de atributos na view
        model.addAttribute("currentPage", "commercial");
        model.addAttribute("totalQuotes", totalQuotes);
        model.addAttribute("totalClients", totalClients);
        model.addAttribute("totalReceipts", totalReceipts);
        model.addAttribute("valorVendido", valorVendido);

        model.addAttribute("countPending", pending);
        model.addAttribute("countApproved", approved);
        model.addAttribute("countCancelled", cancelled);
        model.addAttribute("countExpired", expired);

        // Lista histórica ordenada dos últimos 6 orçamentos gerados
        List<Quote> recentQuotes = allQuotes.stream()
                .sorted((q1, q2) -> q2.getDateCreated().compareTo(q1.getDateCreated()))
                .limit(6)
                .toList();
        model.addAttribute("recentQuotes", recentQuotes);

        return "commercial-dashboard";
    }
}