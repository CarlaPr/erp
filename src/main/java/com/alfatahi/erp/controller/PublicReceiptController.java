    package com.alfatahi.erp.controller;

import com.alfatahi.erp.service.ReceiptService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/public/receipts")
public class PublicReceiptController {

    private final ReceiptService receiptService;

    public PublicReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/{token}")
    public String view(@PathVariable String token, Model model) {
        try {
            model.addAttribute("receipt", receiptService.publicView(token));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return "public-receipt";
    }

    @PostMapping("/{token}/sign")
    @ResponseBody
    public ResponseEntity<?> sign(@PathVariable String token, @RequestBody Map<String, String> payload) {
        try {
            receiptService.sign(token, payload.get("signature"));
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{token}/photos/{photoId}")
    public ResponseEntity<byte[]> photo(@PathVariable String token, @PathVariable UUID photoId) {
        return receiptService.publicPhoto(token, photoId)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.contentType()))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                        .body(p.content()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
