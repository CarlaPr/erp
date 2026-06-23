package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.entity.QuoteItem;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.QuoteRepository;
import com.alfatahi.erp.service.QuoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteRepository quoteRepo;
    private final ClientRepository clientRepo;
    private final QuoteService quoteService;

    private final com.alfatahi.erp.repository.ProfileRepository profileRepo;

    public QuoteController(QuoteRepository quoteRepo, ClientRepository clientRepo, QuoteService quoteService, com.alfatahi.erp.repository.ProfileRepository profileRepo) {
        this.quoteRepo = quoteRepo;
        this.clientRepo = clientRepo;
        this.quoteService = quoteService;
        this.profileRepo = profileRepo;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String name,
            Model model) {

        // 1. Validação automática de orçamentos pendentes com mais de 1 mês
        LocalDate limiteValidade = LocalDate.now().minusMonths(1);
        List<Quote> todosOrcamentos = quoteRepo.findAll();

        boolean houveramAlteracoes = false;
        for (Quote q : todosOrcamentos) {
            if ("pending".equals(q.getStatus()) && q.getDateCreated() != null && q.getDateCreated().isBefore(limiteValidade)) {
                q.setStatus("canceled");
                houveramAlteracoes = true;
            }
        }
        if (houveramAlteracoes) {
            quoteRepo.saveAll(todosOrcamentos);
        }

        // 2. Ordenação rigorosa: Mais recente primeiro (Data e ID/Número)
        todosOrcamentos.sort((q1, q2) -> {
            if (q1.getDateCreated() == null || q2.getDateCreated() == null) return 0;
            int dataCompare = q2.getDateCreated().compareTo(q1.getDateCreated());
            if (dataCompare != 0) return dataCompare;
            if (q1.getNumber() == null) return 1;
            if (q2.getNumber() == null) return -1;
            return q2.getNumber().compareTo(q1.getNumber());
        });

        // 3. Define o mês atual como filtro padrão
        if (month == null || month.isEmpty()) {
            if (!"all".equals(month)) {
                month = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDate.now());
            }
        }

        // 4. Filtragem por Stream
        final String finalMonth = month;
        List<Quote> filteredList = todosOrcamentos.stream().filter(q -> {
            if (status != null && !status.isEmpty() && !q.getStatus().equals(status)) return false;
            if (number != null && !number.isEmpty() && !q.getNumber().toLowerCase().contains(number.toLowerCase())) return false;
            if (name != null && !name.isEmpty() && q.getClient() != null && !q.getClient().getName().toLowerCase().contains(name.toLowerCase())) return false;

            if (finalMonth != null && !finalMonth.equals("all")) {
                if (q.getDateCreated() == null) return false;
                String quoteYearMonth = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM").format(q.getDateCreated());
                if (!quoteYearMonth.equals(finalMonth)) return false;
            }
            return true;
        }).collect(java.util.stream.Collectors.toList());

        // =========================================================================
        // REQUISITO: GERAÇÃO DINÂMICA DOS MESES DO ANO (SEM COMPONENTES FIXOS)
        // =========================================================================
        List<java.util.Map<String, String>> disponiveis = new java.util.ArrayList<>();
        LocalDate dataLoop = LocalDate.now();
        // Loop para gerar os últimos 12 meses dinamicamente a partir de hoje
        for (int i = 0; i < 12; i++) {
            LocalDate target = dataLoop.minusMonths(i);
            String val = target.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            String label = target.format(java.time.format.DateTimeFormatter.ofPattern("MMMM / yyyy", new java.util.Locale("pt", "BR")));
            label = label.substring(0, 1).toUpperCase() + label.substring(1); // Capitaliza a primeira letra

            java.util.Map<String, String> itemMes = new java.util.HashMap<>();
            itemMes.put("value", val);
            itemMes.put("label", i == 0 ? label + " (Mês Atual)" : label);
            disponiveis.add(itemMes);
        }

        com.alfatahi.erp.entity.Profile profile = profileRepo.findAll().stream().findFirst().orElseGet(() -> {
            com.alfatahi.erp.entity.Profile p = new com.alfatahi.erp.entity.Profile();
            p.setCompanyName("Alfa Tahi");
            return profileRepo.save(p);
        });

        // Envio seguro ao Thymeleaf
        model.addAttribute("quote", new Quote());
        model.addAttribute("currentPage", "quotes");
        model.addAttribute("quotes", filteredList);
        model.addAttribute("clients", clientRepo.findAll());
        model.addAttribute("profile", profile);
        model.addAttribute("availableMonths", disponiveis); // Nova lista dinâmica ativa
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedNumber", number);
        model.addAttribute("selectedName", name);
        model.addAttribute("selectedFilterStatus", status);

        return "quotes";
    }

    @GetMapping("/view-data/{id}")
    @ResponseBody
    public ResponseEntity<Quote> getQuoteData(@PathVariable UUID id) {
        return ResponseEntity.ok(quoteRepo.findById(id).orElseThrow());
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> saveAjax(@RequestBody Quote quote) {

        if (quote.getNumber() == null || quote.getNumber().isEmpty()) {
            long nextQuoteNum = quoteRepo.count() + 1;
            quote.setNumber(String.format("ORC-%02d", nextQuoteNum));
        }

        if (quote.getDateCreated() == null) {
            quote.setDateCreated(LocalDate.now());
        }

        if (quote.getItems() != null) {
            for (QuoteItem item : quote.getItems()) {
                item.setQuote(quote);
            }
        }
        quoteRepo.save(quote);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approve/{id}")
    @ResponseBody
    public ResponseEntity<?> approve(@PathVariable UUID id) {
        quoteService.approveAndGenerateIntegrations(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody String reason) {
        Quote quote = quoteRepo.findById(id).orElseThrow();
        quote.setStatus("cancelled");
        quote.setObservations(quote.getObservations() + "\n[Motivo Cancelamento: " + reason + "]");
        quoteRepo.save(quote);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/add-client-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<com.alfatahi.erp.entity.Client> addClientAjax(@RequestBody com.alfatahi.erp.entity.Client client) {
        // Define o cliente como ativo por padrão no sistema
        client.setActive(true);
        com.alfatahi.erp.entity.Client savedClient = clientRepo.save(client);
        return ResponseEntity.ok(savedClient);
    }
}