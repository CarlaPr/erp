package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.CashLedgerReport;
import com.alfatahi.erp.service.CashLedgerPdfService;
import com.alfatahi.erp.service.CashLedgerService;
import com.alfatahi.erp.service.FinancialPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/cash-ledger")
public class CashLedgerController {

    private static final Logger log = LoggerFactory.getLogger(CashLedgerController.class);
    private final CashLedgerService cashLedgerService;
    private final CashLedgerPdfService cashLedgerPdfService;

    public CashLedgerController(CashLedgerService cashLedgerService, CashLedgerPdfService cashLedgerPdfService) {
        this.cashLedgerService = cashLedgerService;
        this.cashLedgerPdfService = cashLedgerPdfService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String month,
            Model model, RedirectAttributes redirectAttributes) {
        FinancialPeriod period;
        try {
            period = selectPeriod(from, to, month);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cash-ledger";
        }
        period.addTo(model);
        CashLedgerReport report = cashLedgerService.buildReport(period.from(), period.to());

        model.addAttribute("currentPage",    "cash-ledger");
        model.addAttribute("entries",        report.entries());
        model.addAttribute("from",           report.from());
        model.addAttribute("to",             report.to());
        model.addAttribute("openingBalance", report.openingBalance());
        model.addAttribute("totalEntradas",  report.totalEntradas());
        model.addAttribute("totalSaidas",    report.totalSaidas());
        model.addAttribute("saldoFinal",     report.saldoFinal());
        model.addAttribute("totalTaxas",     report.totalTaxas());
        model.addAttribute("saldoTotalAtual", report.closingBalances().total());
        model.addAttribute("saldoBancoAtual", report.closingBalances().bank());
        model.addAttribute("saldoDinheiroAtual", report.closingBalances().cash());
        model.addAttribute("openingBankBalance", report.openingBalances().bank());
        model.addAttribute("openingCashBalance", report.openingBalances().cash());
        return "cash-ledger";
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String month) {
        try {
            FinancialPeriod period = selectPeriod(from, to, month);
            CashLedgerReport report = cashLedgerService.buildReport(period.from(), period.to());
            CashLedgerPdfService.Pdf pdf = cashLedgerPdfService.render(report);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(pdf.fileName(), StandardCharsets.UTF_8).build().toString())
                    .body(pdf.bytes());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body(e.getMessage().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Erro ao gerar PDF do fluxo de caixa", e);
            return ResponseEntity.internalServerError()
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body("Não foi possível gerar o PDF do fluxo de caixa. Tente novamente.".getBytes(StandardCharsets.UTF_8));
        }
    }

    private FinancialPeriod selectPeriod(LocalDate from, LocalDate to, String month) {
        FinancialPeriod period = FinancialPeriod.select(month, false, from, to);
        FinancialPeriod current = FinancialPeriod.current();
        if (period.from() == null || period.to() == null) {
            period = new FinancialPeriod(null,
                    period.from() != null ? period.from() : current.from(),
                    period.to() != null ? period.to() : current.to());
        }
        if (period.from().isAfter(period.to())) {
            throw new IllegalArgumentException("A data inicial deve ser anterior ou igual à data final.");
        }
        return period;
    }
}
