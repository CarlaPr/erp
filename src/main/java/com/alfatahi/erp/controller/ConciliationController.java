package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.BankAccount;
import com.alfatahi.erp.entity.BankTransaction;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.BankTransactionRepository;
import com.alfatahi.erp.repository.BankAccountRepository;
import com.alfatahi.erp.service.OfxParserService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/conciliation")
public class ConciliationController {

    private static final BigDecimal TOLERANCIA = new BigDecimal("0.05");
    private static final long JANELA_DIAS = 5;
    private static final BigDecimal FAIXA_MIN = new BigDecimal("0.50");
    private static final BigDecimal FAIXA_MAX = new BigDecimal("1.10");

    private final BankTransactionRepository bankRepo;
    private final AccountsPayableRepository payRepo;
    private final AccountsReceivableRepository recRepo;
    private final OfxParserService ofxParserService;
    private final BankAccountRepository bankAccountRepository;

    public ConciliationController(BankTransactionRepository bankRepo, AccountsPayableRepository payRepo,
                                  AccountsReceivableRepository recRepo, OfxParserService ofxParserService,
                                  BankAccountRepository bankAccountRepository) {
        this.bankRepo = bankRepo;
        this.payRepo = payRepo;
        this.recRepo = recRepo;
        this.ofxParserService = ofxParserService;
        this.bankAccountRepository = bankAccountRepository;
    }

    @GetMapping
    public String index(Model model) {
        List<BankTransaction> transactions = bankRepo.findAllByOrderByTransactionDateDesc();

        BigDecimal saldoConciliado = BigDecimal.ZERO;
        BigDecimal entradasPendentes = BigDecimal.ZERO;
        BigDecimal saidasPendentes = BigDecimal.ZERO;
        long divergentesCount = 0;

        for (BankTransaction t : transactions) {
            if ("conciliated".equals(t.getStatus())) {
                if ("IN".equals(t.getType())) saldoConciliado = saldoConciliado.add(t.getAmount());
                else saldoConciliado = saldoConciliado.subtract(t.getAmount());
                if (t.getDivergenceAmount().compareTo(BigDecimal.ZERO) != 0) divergentesCount++;
            } else {
                if ("IN".equals(t.getType())) entradasPendentes = entradasPendentes.add(t.getAmount());
                else saidasPendentes = saidasPendentes.add(t.getAmount());
            }
        }

        model.addAttribute("currentPage", "conciliation");
        model.addAttribute("transactions", transactions);
        model.addAttribute("saldoConciliado", saldoConciliado);
        model.addAttribute("entradasPendentes", entradasPendentes);
        model.addAttribute("saidasPendentes", saidasPendentes);
        model.addAttribute("divergentesCount", divergentesCount);
        model.addAttribute("newTx", new BankTransaction());

        // LINHA NOVA AQUI:
        model.addAttribute("bankAccounts", bankAccountRepository.findAll());

        return "conciliation";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newTx") BankTransaction tx) {
        bankRepo.save(tx);
        return "redirect:/conciliation";
    }

    @PostMapping("/toggle/{id}")
    @Transactional
    public String toggleStatus(@PathVariable("id") UUID id) {
        BankTransaction tx = bankRepo.findById(id).orElse(null);
        if (tx == null) {
            return "redirect:/conciliation";
        }

        boolean estavaConciliado = "conciliated".equals(tx.getStatus());

        if (estavaConciliado) {
            revertLink(tx);
            tx.setStatus("pending");
            tx.setDivergenceAmount(BigDecimal.ZERO);
            tx.setReconciliationNote(null);
        } else {

            attemptMatch(tx);
            tx.setStatus("conciliated");
        }

        BankAccount account = tx.getBankAccount();
            if (account != null) {
                if (!estavaConciliado) {
                    if ("IN".equals(tx.getType())) {
                        account.setCurrentBalance(account.getCurrentBalance().add(tx.getAmount()));
                    } else {
                        account.setCurrentBalance(account.getCurrentBalance().subtract(tx.getAmount()));
                    }
                } else {
                    if ("IN".equals(tx.getType())) {
                        account.setCurrentBalance(account.getCurrentBalance().subtract(tx.getAmount()));
                    } else {
                        account.setCurrentBalance(account.getCurrentBalance().add(tx.getAmount()));
                    }
                }
            bankAccountRepository.save(account);
        }
        if (tx.getBankAccount() != null
                && tx.getBankAccount().getId() != null) {
                BankAccount ba = bankAccountRepository
                        .findById(tx.getBankAccount().getId()).orElse(null);
                tx.setBankAccount(ba);
            }

        bankRepo.save(tx);
        return "redirect:/conciliation";
    }

    @PostMapping("/upload-ofx")
    @Transactional
    public String uploadOfx(@RequestParam("file") MultipartFile file,
                            @RequestParam("bankAccountId") UUID bankAccountId, // NOVO PARÂMETRO
                            RedirectAttributes redirectAttributes) {
        try {
            BankAccount account = bankAccountRepository.findById(bankAccountId)
                    .orElseThrow(() -> new RuntimeException("Conta bancária não encontrada."));

            List<BankTransaction> transactions = ofxParserService.parse(file);

            int novos = 0, duplicados = 0, conciliados = 0, divergentes = 0;

            for (BankTransaction tx : transactions) {
                tx.setBankAccount(account);

                if (tx.getExternalId() != null && bankRepo.existsByExternalId(tx.getExternalId())) {
                    duplicados++;
                    continue;
                }

                attemptMatch(tx);
                if ("conciliated".equals(tx.getStatus())) {
                    conciliados++;
                    if (tx.getDivergenceAmount().compareTo(BigDecimal.ZERO) != 0) divergentes++;
                }
                bankRepo.save(tx);
                novos++;
            }

            StringBuilder msg = new StringBuilder(novos + " lançamento(s) importado(s)");
            if (conciliados > 0) msg.append(", ").append(conciliados).append(" conciliado(s) automaticamente");
            if (divergentes > 0) msg.append(" (").append(divergentes).append(" com divergência — revisar)");
            if (duplicados > 0) msg.append(". ").append(duplicados).append(" já tinham sido importados antes e foram ignorados");
            msg.append(".");

            redirectAttributes.addFlashAttribute("successMsg", msg.toString());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Erro ao processar o arquivo OFX: " + e.getMessage());
        }
        return "redirect:/conciliation";
    }

    @PostMapping("/add-account")
    public String addAccount(@RequestParam String name,
                             @RequestParam String bankName,
                             @RequestParam String type,
                             RedirectAttributes redirectAttributes) {
        BankAccount acc = new BankAccount();
        acc.setName(name);
        acc.setBankName(bankName);
        acc.setType(type);
        acc.setCurrentBalance(BigDecimal.ZERO);

        bankAccountRepository.save(acc);

        redirectAttributes.addFlashAttribute("successMsg", "Conta bancária adicionada com sucesso!");
        return "redirect:/conciliation";
    }

    private void attemptMatch(BankTransaction tx) {
        if ("IN".equals(tx.getType())) {
            AccountsReceivable best = null;
            BigDecimal bestDiff = null;

            for (AccountsReceivable r : recRepo.findAllByOrderByDueDateAsc()) {

                if (!"received".equals(r.getStatus()) && !"partial".equals(r.getStatus())) continue;
                if ("CONCILIADO".equals(r.getReconciliationStatus())) continue; // ja resolvido
                if (r.getDueDate() == null) continue;

                long daysDiff = Math.abs(ChronoUnit.DAYS.between(r.getDueDate(), tx.getTransactionDate()));
                if (daysDiff > JANELA_DIAS) continue;

                BigDecimal expectedNet = r.getTotalAmount().subtract(r.getFeeAmount());
                if (expectedNet.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal ratio = tx.getAmount().divide(expectedNet, 4, RoundingMode.HALF_UP);
                if (ratio.compareTo(FAIXA_MIN) < 0 || ratio.compareTo(FAIXA_MAX) > 0) continue;

                BigDecimal diff = expectedNet.subtract(tx.getAmount()).abs();
                if (bestDiff == null || diff.compareTo(bestDiff) < 0) {
                    best = r;
                    bestDiff = diff;
                }
            }

            if (best != null) {
                applyReceivableMatch(tx, best, bestDiff);
            }

        } else if ("OUT".equals(tx.getType())) {
            AccountsPayable best = null;
            BigDecimal bestDiff = null;

            for (AccountsPayable p : payRepo.findAllByOrderByDueDateAsc()) {
                if (!"paid".equals(p.getStatus()) && !"partial".equals(p.getStatus())) continue;
                if ("CONCILIADO".equals(p.getReconciliationStatus())) continue;
                if (p.getDueDate() == null) continue;

                long daysDiff = Math.abs(ChronoUnit.DAYS.between(p.getDueDate(), tx.getTransactionDate()));
                if (daysDiff > JANELA_DIAS) continue;

                BigDecimal expected = p.getTotalAmount();
                if (expected == null || expected.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal ratio = tx.getAmount().divide(expected, 4, RoundingMode.HALF_UP);
                if (ratio.compareTo(FAIXA_MIN) < 0 || ratio.compareTo(FAIXA_MAX) > 0) continue;

                BigDecimal diff = expected.subtract(tx.getAmount()).abs();
                if (bestDiff == null || diff.compareTo(bestDiff) < 0) {
                    best = p;
                    bestDiff = diff;
                }
            }

            if (best != null) {
                applyPayableMatch(tx, best, bestDiff);
            }
        }
    }

    private void applyReceivableMatch(BankTransaction tx, AccountsReceivable r, BigDecimal diff) {
        BigDecimal expectedNet = r.getTotalAmount().subtract(r.getFeeAmount());
        boolean divergente = diff.compareTo(TOLERANCIA) > 0;

        r.setReconciliationStatus(divergente ? "DIVERGENTE" : "CONCILIADO");
        recRepo.save(r);

        tx.setMatchedReceivable(r);
        tx.setDivergenceAmount(divergente ? expectedNet.subtract(tx.getAmount()) : BigDecimal.ZERO);
        tx.setDescription(tx.getDescription() + " (Vínculo: " + r.getDescription() + ")");
        tx.setReconciliationNote(divergente
                ? String.format("DIVERGENTE: esperado líquido R$ %.2f, recebido R$ %.2f no extrato (diferença R$ %.2f)",
                expectedNet, tx.getAmount(), expectedNet.subtract(tx.getAmount()))
                : (r.getFeeAmount().compareTo(BigDecimal.ZERO) > 0
                   ? String.format("Conciliado — diferença de R$ %.2f já explicada pela taxa de cartão", r.getFeeAmount())
                   : "Conciliado sem divergência"));
    }

    private void applyPayableMatch(BankTransaction tx, AccountsPayable p, BigDecimal diff) {
        boolean divergente = diff.compareTo(TOLERANCIA) > 0;

        p.setReconciliationStatus(divergente ? "DIVERGENTE" : "CONCILIADO");
        payRepo.save(p);

        tx.setMatchedPayable(p);
        tx.setDivergenceAmount(divergente ? p.getTotalAmount().subtract(tx.getAmount()) : BigDecimal.ZERO);
        tx.setDescription(tx.getDescription() + " (Vínculo: " + p.getDescription() + ")");
        tx.setReconciliationNote(divergente
                ? String.format("DIVERGENTE: esperado R$ %.2f, pago R$ %.2f no extrato (diferença R$ %.2f)",
                p.getTotalAmount(), tx.getAmount(), p.getTotalAmount().subtract(tx.getAmount()))
                : "Conciliado sem divergência");
    }

    private void revertLink(BankTransaction tx) {
        if (tx.getMatchedReceivable() != null) {
            AccountsReceivable r = tx.getMatchedReceivable();
            r.setReconciliationStatus("NAO_CONCILIADO");
            recRepo.save(r);
            tx.setMatchedReceivable(null);
        }
        if (tx.getMatchedPayable() != null) {
            AccountsPayable p = tx.getMatchedPayable();
            p.setReconciliationStatus("NAO_CONCILIADO");
            payRepo.save(p);
            tx.setMatchedPayable(null);
        }
    }
}
