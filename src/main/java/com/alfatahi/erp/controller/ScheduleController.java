package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.ScheduleDto;
import com.alfatahi.erp.dto.ScheduleSaveRequest;
import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Módulo AGENDA COMERCIAL (Portal de Vendas).
 * Não é uma tela operacional nova: apenas organiza data/prazo/status de execução
 * dos serviços já aprovados, sempre em torno da OS (WorkOrder) já existente.
 */
@Controller
@RequestMapping("/agenda")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AppUserRepository appUserRepo;

    public ScheduleController(ScheduleService scheduleService, AppUserRepository appUserRepo) {
        this.scheduleService = scheduleService;
        this.appUserRepo = appUserRepo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        List<ScheduleDto> schedules = scheduleService.listAllDto();

        List<String> vendedores = appUserRepo.findByRole("VENDAS").stream()
                .map(AppUser::getUsername)
                .collect(Collectors.toList());

        model.addAttribute("currentPage", "agenda");
        model.addAttribute("schedules", schedules);
        model.addAttribute("vendedores", vendedores);
        model.addAttribute("kpis", scheduleService.getKpis());

        return "agenda";
    }

    @GetMapping("/view-data/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<ScheduleDto> getViewData(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.findDto(id));
    }

    @PostMapping(value = "/save-ajax", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveAjax(@RequestBody ScheduleSaveRequest request) {
        try {
            String warning = scheduleService.save(request);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", true);
            if (warning != null) {
                body.put("warning", warning);
            }
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("ok", false);
            body.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(body);
        }
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody(required = false) String reason) {
        try {
            scheduleService.cancel(id, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao cancelar agendamento: " + e.getMessage());
        }
    }
}
