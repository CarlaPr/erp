package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.ScheduleDto;
import com.alfatahi.erp.dto.ScheduleOccurrenceDto;
import com.alfatahi.erp.dto.ScheduleOccurrenceSaveRequest;
import com.alfatahi.erp.dto.ScheduleSaveRequest;
import com.alfatahi.erp.dto.TechnicalVisitDto;
import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.service.ScheduleService;
import com.alfatahi.erp.service.TechnicalVisitService;
import com.alfatahi.erp.util.SecurityUtils;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/agenda")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TechnicalVisitService technicalVisitService;
    private final AppUserRepository appUserRepo;
    private final TemplateEngine templateEngine;

    public ScheduleController(ScheduleService scheduleService, TechnicalVisitService technicalVisitService,
                               AppUserRepository appUserRepo, TemplateEngine templateEngine) {
        this.scheduleService = scheduleService;
        this.technicalVisitService = technicalVisitService;
        this.appUserRepo = appUserRepo;
        this.templateEngine = templateEngine;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        boolean isTecnico = SecurityUtils.isTecnico();


        List<ScheduleDto> schedules = scheduleService.listAllDto();
        List<TechnicalVisitDto> technicalVisits = technicalVisitService.listAllDto();

        List<String> vendedores = appUserRepo.findByRole("VENDAS").stream()
                .map(AppUser::getUsername)
                .collect(Collectors.toList());

        model.addAttribute("currentPage", "agenda");
        model.addAttribute("isTecnico", isTecnico);
        model.addAttribute("schedules", schedules);
        model.addAttribute("technicalVisits", technicalVisits);
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
        if (SecurityUtils.isTecnico()) {
            return forbidden();
        }
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


    @PostMapping(value = "/{id}/deadline", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> changeDeadline(@PathVariable UUID id,
                                                                @RequestBody Map<String, String> body) {
        if (SecurityUtils.isTecnico()) {
            return forbidden();
        }
        try {
            LocalDate newDeadline = LocalDate.parse(body.get("deadlineDate"));
            String reason = body.get("reason");
            scheduleService.changeDeadline(id, newDeadline, reason);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (DateTimeParseException e) {
            return badRequest("Data de prazo inválida.");
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return internalError("Erro ao alterar o prazo.");
        }
    }


    @PostMapping(value = "/occurrences/add", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addOccurrence(@RequestBody ScheduleOccurrenceSaveRequest request) {
        if (SecurityUtils.isTecnico()) {
            return forbidden();
        }
        try {
            ScheduleOccurrenceDto occ = scheduleService.addOccurrence(request);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            resp.put("occurrence", occ);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return internalError("Erro ao adicionar dia de agendamento.");
        }
    }


    @PostMapping(value = "/occurrences/update", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateOccurrence(@RequestBody ScheduleOccurrenceSaveRequest request) {
        if (SecurityUtils.isTecnico()) {
            return forbidden();
        }
        try {
            ScheduleOccurrenceDto occ = scheduleService.updateOccurrence(request);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            resp.put("occurrence", occ);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return internalError("Erro ao editar dia de agendamento.");
        }
    }


    @PostMapping(value = "/occurrences/{occurrenceId}/remove", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeOccurrence(@PathVariable UUID occurrenceId,
                                                                  @RequestBody(required = false) Map<String, String> body) {
        if (SecurityUtils.isTecnico()) {
            return forbidden();
        }
        try {
            String reason = body != null ? body.get("reason") : null;
            scheduleService.removeOccurrence(occurrenceId, reason);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return internalError("Erro ao remover dia de agendamento.");
        }
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody(required = false) String reason) {
        if (SecurityUtils.isTecnico()) {
            return forbidden();
        }
        try {
            scheduleService.cancel(id, reason);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao cancelar agendamento: " + e.getMessage());
        }
    }


    @PostMapping(value = "/{id}/technician-note", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addTechnicianNote(@PathVariable UUID id,
                                                                   @RequestBody Map<String, String> body) {
        try {
            scheduleService.addTechnicianNote(id, body.get("notes"));
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            return ResponseEntity.ok(resp);
        } catch (SecurityException e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.status(403).body(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", false);
            resp.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(resp);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", false);
            resp.put("error", "Erro ao registrar observação.");
            return ResponseEntity.internalServerError().body(resp);
        }
    }


    @GetMapping("/roteiro-pdf/{date}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> roteiroPdf(@PathVariable String date) {
        try {
            LocalDate day;
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest().build();
            }



            List<ScheduleDto> schedules = scheduleService.listAllDto();

            List<RoteiroItemView> items = new ArrayList<>();
            for (ScheduleDto s : schedules) {
                if (s.getOccurrences() != null && !s.getOccurrences().isEmpty()) {
                    for (var occ : s.getOccurrences()) {
                        if (day.equals(occ.getOccurrenceDate())) {
                            items.add(toRoteiroRow(s, occ));
                        }
                    }
                } else if (day.equals(s.getScheduledDate())) {
                    items.add(toRoteiroRow(s, null));
                }
            }

            for (TechnicalVisitDto v : technicalVisitService.listAllDto()) {
                if (day.equals(v.getVisitDate())) {
                    items.add(toRoteiroRow(v));
                }
            }

            items.sort(Comparator.comparing(RoteiroItemView::getSortTime,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            Context ctx = new Context(Locale.forLanguageTag("pt-BR"));
            ctx.setVariable("dataFormatada", day.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            ctx.setVariable("items", items);
            ctx.setVariable("totalItems", items.size());

            String html = templateEngine.process("agenda-roteiro-pdf", ctx);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFonts(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            byte[] pdfBytes = out.toByteArray();

            String fileName = "Roteiro " + day.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", "Seu perfil não tem permissão para esta ação.");
        return ResponseEntity.status(403).body(body);
    }

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message);
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> internalError(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("error", message);
        return ResponseEntity.internalServerError().body(body);
    }

    private void registerFonts(PdfRendererBuilder builder) {
        int[] weights = {400, 500, 600, 700, 800, 900};
        for (int weight : weights) {
            String path = "/fonts/Inter-" + weight + ".ttf";


            if (getClass().getResource(path) != null) {
                builder.useFont(() -> getClass().getResourceAsStream(path), "Inter", weight,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }
        }
    }

    private RoteiroItemView toRoteiroRow(ScheduleDto s, ScheduleOccurrenceDto occ) {
        RoteiroItemView r = new RoteiroItemView();
        r.setClientName(nvl(s.getClientName(), "Não informado"));
        r.setClientAddress(nvl(s.getClientAddress(), "-"));
        r.setQuoteNumber(nvl(s.getQuoteNumber(), "-"));
        r.setWorkOrderNumber(nvl(s.getWorkOrderNumber(), "-"));

        LocalTime time = occ != null ? occ.getOccurrenceTime() : s.getScheduledTime();
        r.setSortTime(time);
        r.setTimeFormatted(time != null ? time.toString().substring(0, 5) : "A definir");

        String occTeam = occ != null ? occ.getTeam() : null;
        String occResponsible = occ != null ? occ.getResponsible() : null;
        String team = (occTeam != null && !occTeam.isBlank()) ? occTeam
                : (occResponsible != null && !occResponsible.isBlank()) ? occResponsible
                : (s.getTeam() != null && !s.getTeam().isBlank() ? s.getTeam() : s.getResponsible());
        r.setTeamOrResponsible(nvl(team, "-"));

        r.setItems((s.getServiceDetails() != null && !s.getServiceDetails().isEmpty())
                ? s.getServiceDetails()
                : List.of(nvl(s.getServiceType(), "Serviço não detalhado")));
        r.setObservations(occ != null && occ.getObservations() != null ? occ.getObservations() : s.getObservations());
        r.setVisit(false);
        r.setTypeLabel("Execução de Serviço");
        return r;
    }

    private RoteiroItemView toRoteiroRow(TechnicalVisitDto v) {
        RoteiroItemView r = new RoteiroItemView();
        r.setClientName(nvl(v.getClientName(), "Não informado"));
        r.setClientAddress(nvl(v.getClientAddress(), "-"));
        r.setQuoteNumber(nvl(v.getQuoteNumber(), "-"));
        r.setWorkOrderNumber("-");
        r.setSortTime(v.getVisitTime());
        r.setTimeFormatted(v.getVisitTime() != null ? v.getVisitTime().toString().substring(0, 5) : "A definir");
        r.setTeamOrResponsible("-");
        r.setItems(List.of("Visita técnica ao cliente"));
        r.setObservations(v.getNotes());
        r.setVisit(true);
        r.setTypeLabel("Visita Técnica");
        return r;
    }

    private String nvl(String v, String def) {
        return (v != null && !v.isBlank()) ? v : def;
    }


    public static class RoteiroItemView {
        private String clientName;
        private String clientAddress;
        private String quoteNumber;
        private String workOrderNumber;
        private String timeFormatted;
        private String teamOrResponsible;
        private List<String> items;
        private String observations;
        private boolean visit;
        private String typeLabel;
        private LocalTime sortTime;

        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }

        public String getClientAddress() { return clientAddress; }
        public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }

        public String getQuoteNumber() { return quoteNumber; }
        public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

        public String getWorkOrderNumber() { return workOrderNumber; }
        public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

        public String getTimeFormatted() { return timeFormatted; }
        public void setTimeFormatted(String timeFormatted) { this.timeFormatted = timeFormatted; }

        public String getTeamOrResponsible() { return teamOrResponsible; }
        public void setTeamOrResponsible(String teamOrResponsible) { this.teamOrResponsible = teamOrResponsible; }

        public List<String> getItems() { return items; }
        public void setItems(List<String> items) { this.items = items; }

        public String getObservations() { return observations; }
        public void setObservations(String observations) { this.observations = observations; }

        public boolean isVisit() { return visit; }
        public void setVisit(boolean visit) { this.visit = visit; }

        public String getTypeLabel() { return typeLabel; }
        public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }

        public LocalTime getSortTime() { return sortTime; }
        public void setSortTime(LocalTime sortTime) { this.sortTime = sortTime; }
    }
}
