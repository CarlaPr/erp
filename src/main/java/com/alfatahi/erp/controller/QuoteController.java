package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.entity.QuoteItem;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.QuoteRepository;
import com.alfatahi.erp.service.QuoteService;
import org.hibernate.Hibernate; // IMPORT NECESSÁRIO
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional; // IMPORT NECESSÁRIO
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteRepository quoteRepo;
    private final ClientRepository clientRepo;
    private final QuoteService quoteService;
    private final com.alfatahi.erp.service.ScheduleService scheduleService;

    private final com.alfatahi.erp.repository.ProfileRepository profileRepo;

    public QuoteController(QuoteRepository quoteRepo, ClientRepository clientRepo, QuoteService quoteService, com.alfatahi.erp.service.ScheduleService scheduleService, com.alfatahi.erp.repository.ProfileRepository profileRepo) {
        this.quoteRepo = quoteRepo;
        this.clientRepo = clientRepo;
        this.quoteService = quoteService;
        this.scheduleService = scheduleService;
        this.profileRepo = profileRepo;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public String index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String name,
            Model model) {


        List<Quote> todosOrcamentos = quoteRepo.findAll();

        todosOrcamentos.sort((q1, q2) -> {
            if (q1.getDateCreated() == null || q2.getDateCreated() == null) return 0;
            int dataCompare = q2.getDateCreated().compareTo(q1.getDateCreated());
            if (dataCompare != 0) return dataCompare;
            if (q1.getNumber() == null) return 1;
            if (q2.getNumber() == null) return -1;
            return q2.getNumber().compareTo(q1.getNumber());
        });

        if (month == null || month.isEmpty()) {
            month = DateTimeFormatter.ofPattern("yyyy-MM").format(LocalDateTime.now());
        }

        // 4. Filtro por Stream
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

        // 5. Geração dinâmica dos últimos 12 meses
        List<java.util.Map<String, String>> disponiveis = new java.util.ArrayList<>();
        java.time.LocalDateTime dataLoop = java.time.LocalDateTime.now(); // Alterado para LocalDateTime

        for (int i = 0; i < 12; i++) {
            java.time.LocalDateTime target = dataLoop.minusMonths(i);
            String val = target.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

            String label = target.format(java.time.format.DateTimeFormatter.ofPattern("MMMM / yyyy", java.util.Locale.forLanguageTag("pt-BR")));
            label = label.substring(0, 1).toUpperCase() + label.substring(1);

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

        model.addAttribute("quote", new Quote());
        model.addAttribute("currentPage", "quotes");
        model.addAttribute("quotes", filteredList);
        model.addAttribute("clients", clientRepo.findAll());
        model.addAttribute("profile", profile);
        model.addAttribute("availableMonths", disponiveis);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedNumber", number);
        model.addAttribute("selectedName", name);
        model.addAttribute("selectedFilterStatus", status);

        return "quotes";
    }

    @GetMapping("/view-data/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<Quote> getQuoteData(@PathVariable UUID id) {
        Quote quote = quoteRepo.findById(id).orElseThrow();

        Hibernate.initialize(quote.getItems());

        if (quote.getWorkOrder() != null) {
            Hibernate.initialize(quote.getWorkOrder().getItems());
        }

        return ResponseEntity.ok(quote);
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> saveAjax(@RequestBody Quote quote) {

        if (quote.getId() != null) {
            Quote existing = quoteRepo.findById(quote.getId()).orElseThrow();
            existing.setClient(quote.getClient());

            if (quote.getDateCreated() != null) {
                existing.setDateCreated(quote.getDateCreated());
            }
            existing.setPaymentMethod(quote.getPaymentMethod());
            existing.setInstallments(quote.getInstallments());
            existing.setObservations(quote.getObservations());
            existing.setWarranty(quote.getWarranty());
            existing.setTotalValue(quote.getTotalValue());
            existing.setItems(quote.getItems());
            for (QuoteItem item : existing.getItems()) {
                item.setQuote(existing);
            }
            quoteRepo.save(existing);
            return ResponseEntity.ok().build();
        }

        if (quote.getNumber() == null || quote.getNumber().isEmpty()) {
            int next = quoteRepo.findMaxQuoteSequence() + 1;
            quote.setNumber(String.format("ORC-%04d", next));
        }

        if (quote.getDateCreated() == null) {
            quote.setDateCreated(java.time.LocalDateTime.now());
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
        try {
            quoteService.approveQuote(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao aprovar: " + e.getMessage());
        }
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody String reason) {
        Quote quote = quoteRepo.findById(id).orElseThrow();
        quote.setStatus("cancelled");

        String obs = quote.getObservations() != null ? quote.getObservations() : "";
        quote.setObservations(obs + "\n[Motivo Cancelamento: " + reason + "]");

        quoteRepo.save(quote);

        scheduleService.onQuoteCancelled(id);

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/add-client-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<com.alfatahi.erp.entity.Client> addClientAjax(@RequestBody com.alfatahi.erp.entity.Client client) {
        client.setIsActive(true);
        com.alfatahi.erp.entity.Client savedClient = clientRepo.save(client);
        return ResponseEntity.ok(savedClient);
    }
}