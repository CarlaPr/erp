package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.TechnicalVisitDto;
import com.alfatahi.erp.dto.TechnicalVisitSaveRequest;
import com.alfatahi.erp.service.TechnicalVisitService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class TechnicalVisitController {

    private final TechnicalVisitService technicalVisitService;

    public TechnicalVisitController(TechnicalVisitService technicalVisitService) {
        this.technicalVisitService = technicalVisitService;
    }

    @PostMapping(value = "/quotes/{quoteId}/technical-visits", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> scheduleFromQuote(@PathVariable UUID quoteId,
                                                                   @RequestBody TechnicalVisitSaveRequest request) {
        try {
            TechnicalVisitDto dto = technicalVisitService.scheduleForQuote(quoteId, request);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            body.put("id", dto.getId());
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return errorBody(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return errorBody("Erro ao agendar visita técnica: " + e.getMessage());
        }
    }

    @GetMapping("/agenda/technical-visits/view-data/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<TechnicalVisitDto> getViewData(@PathVariable UUID id) {
        return ResponseEntity.ok(technicalVisitService.findDto(id));
    }

    @PostMapping(value = "/agenda/technical-visits/save-ajax", consumes = "application/json")
    public ResponseEntity<Map<String, Object>> saveAjax(@RequestBody TechnicalVisitSaveRequest request) {
        try {
            technicalVisitService.update(request);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return errorBody(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return errorBody("Erro ao salvar visita técnica: " + e.getMessage());
        }
    }

    @DeleteMapping("/agenda/technical-visits/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            technicalVisitService.delete(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao excluir visita técnica: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> errorBody(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message);
        return ResponseEntity.badRequest().body(body);
    }
}
