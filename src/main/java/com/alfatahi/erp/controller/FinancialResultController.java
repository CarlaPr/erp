package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.FinancialResultFilter;
import com.alfatahi.erp.dto.FinancialResultReport.Movement;
import com.alfatahi.erp.service.FinancialPeriod;
import com.alfatahi.erp.service.FinancialResultService;
import com.alfatahi.erp.service.FinancialResultPdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/reports/financial-result")
public class FinancialResultController {
    private static final Logger log = LoggerFactory.getLogger(FinancialResultController.class);
    private final FinancialResultService service;
    private final FinancialResultPdfService pdfService;
    public FinancialResultController(FinancialResultService service, FinancialResultPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String month,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) UUID workOrderId,
                        @RequestParam(required = false) String type,
                        @RequestParam(required = false) String receiptStatus,
                        @RequestParam(required = false) String paymentStatus,
                        @RequestParam(required = false) String sort, Model model) {
        FinancialResultFilter filter;
        try {
            filter = FinancialResultFilter.select(month, from, to, category, workOrderId, type, receiptStatus, paymentStatus, sort);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período ou filtros inválidos. Informe um intervalo válido.");
        }
        model.addAttribute("currentPage", "financial-result");
        model.addAttribute("filter", filter);
        model.addAttribute("report", service.report(filter));
        model.addAttribute("categories", service.categoryOptions());
        model.addAttribute("orders", service.orderOptions());
        model.addAttribute("periodMode", from == null && to == null ? "month" : "custom");
        model.addAttribute("month", month == null || month.isBlank() ? YearMonth.from(filter.from()).toString() : month);
        model.addAttribute("today", FinancialPeriod.today());
        return "financial-result";
    }

    @GetMapping("/orders/{id}/movements")
    @ResponseBody
    public List<Movement> history(@PathVariable UUID id) { return service.history(id); }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestParam(required = false) String month,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) UUID workOrderId,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) String receiptStatus,
                                      @RequestParam(required = false) String paymentStatus,
                                      @RequestParam(required = false) String sort) {
        try {
            FinancialResultFilter filter = FinancialResultFilter.select(month, from, to, category, workOrderId,
                    type, receiptStatus, paymentStatus, sort);
            FinancialResultPdfService.Pdf pdf = pdfService.render(service.report(filter));
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                            .filename(pdf.fileName(), StandardCharsets.UTF_8).build().toString())
                    .body(pdf.bytes());
        } catch (FinancialResultService.InvalidAllocationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Período ou filtros inválidos. Informe um intervalo válido.");
        } catch (Exception e) {
            log.error("Erro ao gerar PDF de Resultado Financeiro e Rentabilidade", e);
            return ResponseEntity.internalServerError()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("Não foi possível gerar o PDF do Resultado Financeiro. Tente novamente.".getBytes(StandardCharsets.UTF_8));
        }
    }

    @ExceptionHandler(FinancialResultService.InvalidAllocationException.class)
    public ResponseEntity<String> invalidAllocation(FinancialResultService.InvalidAllocationException exception) {
        return ResponseEntity.badRequest().contentType(MediaType.valueOf("text/plain;charset=UTF-8")).body(exception.getMessage());
    }
}
