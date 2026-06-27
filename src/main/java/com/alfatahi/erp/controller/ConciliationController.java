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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/conciliation")
public class ConciliationController {

    private final BankTransactionRepository bankRepo;
    private final AccountsPayableRepository payRepo;
    private final AccountsReceivableRepository recRepo;
    private final OfxParserService ofxParserService;
    private final BankAccountRepository bankAccountRepository;

    // Injetamos todos os repositórios necessários para dar as baixas
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

        for (BankTransaction t : transactions) {
            if ("conciliated".equals(t.getStatus())) {
                if ("IN".equals(t.getType())) saldoConciliado = saldoConciliado.add(t.getAmount());
                else saldoConciliado = saldoConciliado.subtract(t.getAmount());
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
        model.addAttribute("newTx", new BankTransaction());

        return "conciliation";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newTx") BankTransaction tx) {
        bankRepo.save(tx);
        return "redirect:/conciliation";
    }

    @GetMapping("/toggle/{id}")
    @Transactional
    public String toggleStatus(@PathVariable("id") UUID id) {
        // 1. Busca a transação no banco de dados usando o ID recebido
        BankTransaction tx = bankRepo.findById(id).orElse(null);

        if (tx != null) {
            // 2. Define o newStatus (se estava pendente vira conciliado, e vice-versa)
            String newStatus = "conciliated".equals(tx.getStatus()) ? "pending" : "conciliated";
            tx.setStatus(newStatus);

            BankAccount account = tx.getBankAccount();
            if (account != null) {
                // 3. Atualiza o saldo da conta bancária
                if ("conciliated".equals(newStatus)) {
                    // Conciliando: Adiciona se for entrada (IN), subtrai se for saída (OUT)
                    if ("IN".equals(tx.getType())) {
                        account.setCurrentBalance(account.getCurrentBalance().add(tx.getAmount()));
                    } else {
                        account.setCurrentBalance(account.getCurrentBalance().subtract(tx.getAmount()));
                    }
                } else {
                    // Desfazendo a conciliação (Estorno): Subtrai se for entrada, adiciona se for saída
                    if ("IN".equals(tx.getType())) {
                        account.setCurrentBalance(account.getCurrentBalance().subtract(tx.getAmount()));
                    } else {
                        account.setCurrentBalance(account.getCurrentBalance().add(tx.getAmount()));
                    }
                }
                bankAccountRepository.save(account);
            }

            // 4. Salva a transação com o status atualizado
            bankRepo.save(tx);
        }

        return "redirect:/conciliation";
    }

    // =======================================================
    // NOVO: ROBÔ DE IMPORTAÇÃO OFX E BAIXA AUTOMÁTICA
    // =======================================================
    @PostMapping("/upload")
    @Transactional
    public String uploadOfx(@RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        try {
            List<BankTransaction> transactions = ofxParserService.parse(file);
            List<AccountsReceivable> receivables = recRepo.findAll();
            List<AccountsPayable> payables = payRepo.findAll();

            for (BankTransaction tx : transactions) {
                boolean matched = false;

                if ("IN".equals(tx.getType())) {
                    // Procura Contas a Receber compatíveis (Mesmo valor, diferença máx 5 dias)
                    for (AccountsReceivable r : receivables) {
                        if ("pending".equals(r.getStatus()) && r.getTotalAmount().compareTo(tx.getAmount()) == 0) {
                            long daysDiff = Math.abs(ChronoUnit.DAYS.between(r.getDueDate(), tx.getTransactionDate()));
                            if (daysDiff <= 5) {
                                r.setStatus("received");
                                r.setReceivedAmount(tx.getAmount());
                                recRepo.save(r);

                                tx.setStatus("conciliated");
                                tx.setDescription(tx.getDescription() + " (Auto-baixa: " + r.getDescription() + ")");
                                matched = true;

                                redirectAttributes.addFlashAttribute("successMsg",
                                        "Extrato importado com sucesso!");

                                break; // Já encontrou a conta, vai para a próxima transação
                            }
                        }
                    }
                } else if ("OUT".equals(tx.getType())) {
                    // Procura Contas a Pagar compatíveis
                    for (AccountsPayable p : payables) {
                        if ("pending".equals(p.getStatus()) && p.getTotalAmount().compareTo(tx.getAmount()) == 0) {
                            long daysDiff = Math.abs(ChronoUnit.DAYS.between(p.getDueDate(), tx.getTransactionDate()));
                            if (daysDiff <= 5) {
                                p.setStatus("paid");
                                payRepo.save(p);

                                tx.setStatus("conciliated");
                                tx.setDescription(tx.getDescription() + " (Auto-baixa: " + p.getDescription() + ")");
                                matched = true;

                                redirectAttributes.addFlashAttribute("successMsg",
                                        "Extrato importado com sucesso!");
                                break;
                            }
                        }
                    }
                }
                bankRepo.save(tx); // Grava a transação do banco (conciliada ou pendente)
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg",
                    "Erro ao processar o arquivo OFX: " + e.getMessage());
        }
        return "redirect:/conciliation";
    }
}