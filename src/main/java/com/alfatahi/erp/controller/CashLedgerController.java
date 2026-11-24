package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.CashLedgerReport;
import com.alfatahi.erp.service.BankAccountService;
import com.alfatahi.erp.service.CashLedgerPdfService;
import com.alfatahi.erp.service.CashLedgerService;
import com.alfatahi.erp.service.FinancialPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/cash-ledger")
public class CashLedgerController {

    private static final Logger log = LoggerFactory.getLogger(CashLedgerController.class);
    private static final String BANK_ACCOUNTS_REDIRECT = "redirect:/cash-ledger#contas-bancarias";
    private final CashLedgerService cashLedgerService;
    private final CashLedgerPdfService cashLedgerPdfService;
    private final BankAccountService bankAccountService;

    public CashLedgerController(CashLedgerService cashLedgerService, CashLedgerPdfService cashLedgerPdfService,
                                BankAccountService bankAccountService) {
        this.cashLedgerService = cashLedgerService;
        this.cashLedgerPdfService = cashLedgerPdfService;
        this.bankAccountService = bankAccountService;
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
        model.addAttribute("bankAccountBalances", bankAccountService.currentBalances());
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

    @PostMapping("/bank-accounts")
    public String createBankAccount(@RequestParam String name, @RequestParam(required = false) BigDecimal openingBalance,
                                    RedirectAttributes redirectAttributes) {
        try {
            bankAccountService.create(name, openingBalance);
            redirectAttributes.addFlashAttribute("bankAccountSuccess", "Conta bancária cadastrada.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("bankAccountCreateError", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("bankAccountCreateError", "Já existe uma conta bancária com esse nome.");
        }
        return BANK_ACCOUNTS_REDIRECT;
    }

    @PostMapping("/bank-accounts/{id}/edit")
    public String updateBankAccountOpeningBalance(@PathVariable UUID id,
                                                  @RequestParam(required = false) BigDecimal openingBalance,
                                                  RedirectAttributes redirectAttributes) {
        try {
            bankAccountService.updateOpeningBalance(id, openingBalance);
            redirectAttributes.addFlashAttribute("bankAccountSuccess", "Saldo inicial atualizado.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("bankAccountError", e.getMessage());
            redirectAttributes.addFlashAttribute("bankAccountEditId", id);
        }
        return BANK_ACCOUNTS_REDIRECT;
    }

    @PostMapping("/bank-accounts/{id}/delete")
    public String deleteBankAccount(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            bankAccountService.delete(id);
            redirectAttributes.addFlashAttribute("bankAccountSuccess", "Conta bancária excluída.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("bankAccountError", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("bankAccountError", "Esta conta bancária possui movimentações vinculadas e não pode ser excluída.");
        }
        return BANK_ACCOUNTS_REDIRECT;
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
