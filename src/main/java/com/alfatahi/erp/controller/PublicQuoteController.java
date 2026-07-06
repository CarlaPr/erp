package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.repository.QuoteRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller
@RequestMapping("/public/quotes")
public class PublicQuoteController {

    private final QuoteRepository quoteRepo;

    public PublicQuoteController(QuoteRepository quoteRepo) {
        this.quoteRepo = quoteRepo;
    }

    @GetMapping("/{token}")
    public String viewPublicQuote(@PathVariable String token, Model model) {
        Quote quote = quoteRepo.findByPublicToken(token)
                .orElseThrow(() -> new RuntimeException("Orçamento não encontrado ou link inválido."));

        model.addAttribute("quote", quote);
        return "public-quote";
    }

    @PostMapping("/{token}/sign")
    @ResponseBody
    public ResponseEntity<?> signQuote(@PathVariable String token, @RequestBody Map<String, String> payload) {
        Quote quote = quoteRepo.findByPublicToken(token).orElseThrow();
        String signatureBase64 = payload.get("signature");

        if (signatureBase64 != null && !signatureBase64.isEmpty()) {
            quote.setClientSignature(signatureBase64);
            quoteRepo.save(quote);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Assinatura inválida");
    }
}