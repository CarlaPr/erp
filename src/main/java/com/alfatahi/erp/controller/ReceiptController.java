package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.ReceiptSaveRequest;
import com.alfatahi.erp.entity.Receipt;
import com.alfatahi.erp.entity.ReceiptPhoto;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.repository.ReceiptRepository;
import com.alfatahi.erp.service.FinancialPeriod;
import com.alfatahi.erp.service.ReceiptPdfService;
import com.alfatahi.erp.service.ReceiptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {

    private static final Logger log = LoggerFactory.getLogger(ReceiptController.class);

    private final ReceiptRepository receiptRepo;
    private final ReceiptService receiptService;
    private final ReceiptPdfService pdfService;
    private final ClientRepository clientRepo;
    private final ProfileRepository profileRepo;

    public ReceiptController(ReceiptRepository receiptRepo, ReceiptService receiptService,
                             ReceiptPdfService pdfService, ClientRepository clientRepo,
                             ProfileRepository profileRepo) {
        this.receiptRepo = receiptRepo;
        this.receiptService = receiptService;
        this.pdfService = pdfService;
        this.clientRepo = clientRepo;
        this.profileRepo = profileRepo;
    }


    @GetMapping
    @Transactional(readOnly = true)
    public String index(@RequestParam(required = false) String month,
                        @RequestParam(required = false) String number,
                        @RequestParam(required = false) String name,
                        Model model) {

        List<Receipt> all = receiptRepo.findAllForListing();

        FinancialPeriod period = FinancialPeriod.select(month, false, null, null);
        period.addTo(model);
        String selectedMonth = period.reference() == null ? "all" : period.monthValue();

        String numberTerm = number == null ? "" : number.trim().toLowerCase();
        String nameTerm = name == null ? "" : name.trim().toLowerCase();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Receipt r : all) {
            if (!numberTerm.isEmpty() && (r.getNumber() == null || !r.getNumber().toLowerCase().contains(numberTerm))) continue;
            String clientName = r.getClient() != null && r.getClient().getName() != null ? r.getClient().getName() : "";
            if (!nameTerm.isEmpty() && !clientName.toLowerCase().contains(nameTerm)) continue;
            if (!period.contains(r.getIssueDate())) continue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("number", r.getNumber());
            row.put("issueDate", r.getIssueDate());
            row.put("receivedDate", r.getReceivedDate());
            row.put("clientName", clientName.isEmpty() ? "—" : clientName);
            row.put("workOrderNumber", r.getWorkOrder() != null ? r.getWorkOrder().getNumber() : null);
            row.put("total", r.getTotalAmount());
            row.put("publicToken", r.getPublicToken());
            row.put("signed", r.isSigned());
            rows.add(row);
        }

        SortedSet<java.time.YearMonth> references = new TreeSet<>(Comparator.reverseOrder());
        references.add(FinancialPeriod.referenceFor(FinancialPeriod.today()));
        if (period.reference() != null) references.add(period.reference());
        for (Receipt r : all) {
            if (r.getIssueDate() != null) references.add(FinancialPeriod.referenceFor(r.getIssueDate()));
        }
        List<Map<String, String>> available = new ArrayList<>();
        for (java.time.YearMonth reference : references) {
            available.add(Map.of("value", reference.toString(), "label", FinancialPeriod.monthly(reference).label()));
        }

        model.addAttribute("currentPage", "receipts");
        model.addAttribute("receipts", rows);
        model.addAttribute("availableMonths", available);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedNumber", number);
        model.addAttribute("selectedName", name);
        return "receipts";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) UUID workOrderId, Model model, Principal principal) {
        fillFormModel(model, "", workOrderId);
        model.addAttribute("currentUser", principal != null ? principal.getName() : "");
        return "receipt-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        if (!receiptRepo.existsById(id)) return "redirect:/receipts";
        fillFormModel(model, id.toString(), null);
        model.addAttribute("currentUser", "");
        return "receipt-form";
    }

    private void fillFormModel(Model model, String receiptId, UUID preselectedWorkOrderId) {
        List<Map<String, Object>> clients = new ArrayList<>();
        for (var c : clientRepo.findSelectableClients()) {
            clients.add(receiptService.clientMap(c));
        }
        model.addAttribute("currentPage", "receipts");
        model.addAttribute("receiptId", receiptId);
        model.addAttribute("preselectedWorkOrderId", preselectedWorkOrderId != null ? preselectedWorkOrderId.toString() : "");
        model.addAttribute("profiles", profileRepo.findAll());
        model.addAttribute("clients", clients);
        model.addAttribute("defaultDescription", Receipt.DEFAULT_DESCRIPTION);
        model.addAttribute("today", FinancialPeriod.today().toString());
    }


    @GetMapping("/view-data/{id}")
    @ResponseBody
    public ResponseEntity<?> viewData(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(receiptService.view(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/work-orders")
    @ResponseBody
    public List<Map<String, Object>> workOrders() {
        return receiptService.selectableWorkOrders();
    }

    @GetMapping("/work-order-data/{id}")
    @ResponseBody
    public ResponseEntity<?> workOrderData(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(receiptService.workOrderData(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<?> save(@RequestBody ReceiptSaveRequest request, Principal principal) {
        try {
            Receipt saved = receiptService.save(request, principal != null ? principal.getName() : null);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", saved.getId());
            body.put("number", saved.getNumber());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao salvar recibo", e);
            return ResponseEntity.internalServerError().body("Erro ao salvar recibo: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        receiptService.delete(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reset-signature")
    @ResponseBody
    public ResponseEntity<?> resetSignature(@PathVariable UUID id) {
        try {
            receiptService.resetSignature(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }


    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        try {
            ReceiptPdfService.Pdf pdf = pdfService.render(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(pdf.fileName()))
                    .body(pdf.bytes());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Erro ao gerar PDF do recibo {}", id, e);
            String message = "Erro ao gerar PDF: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(message.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String contentDisposition(String fileName) {
        String ascii = java.text.Normalizer.normalize(fileName, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "'");
        String encoded = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "inline; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    @GetMapping("/{id}/photos/{photoId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> photo(@PathVariable UUID id, @PathVariable UUID photoId) {
        Receipt receipt = receiptRepo.findById(id).orElse(null);
        if (receipt == null) return ResponseEntity.notFound().build();
        for (ReceiptPhoto p : receipt.getPhotos()) {
            if (photoId.equals(p.getId())) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.getContentType()))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                        .body(p.getContent());
            }
        }
        return ResponseEntity.notFound().build();
    }
}
