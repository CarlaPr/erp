package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.ScheduleDto;
import com.alfatahi.erp.dto.ScheduleSaveRequest;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.ScheduleHistoryRepository;
import com.alfatahi.erp.repository.ScheduleRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class ScheduleService {

    private static final Pattern AGENDA_NOTE_LINE = Pattern.compile("(?m)^\\[AGENDA].*$\\n?");

    private final ScheduleRepository scheduleRepo;
    private final ScheduleHistoryRepository historyRepo;
    private final WorkOrderRepository workOrderRepo;

    public ScheduleService(ScheduleRepository scheduleRepo, ScheduleHistoryRepository historyRepo,
                            WorkOrderRepository workOrderRepo) {
        this.scheduleRepo = scheduleRepo;
        this.historyRepo = historyRepo;
        this.workOrderRepo = workOrderRepo;
    }

    @Transactional
    public Schedule createFromApprovedQuote(Quote quote, WorkOrder workOrder) {
        Optional<Schedule> existing = scheduleRepo.findByQuoteId(quote.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDateTime approval = quote.getDateApproved() != null ? quote.getDateApproved() : LocalDateTime.now();

        Schedule schedule = new Schedule();
        schedule.setQuote(quote);
        schedule.setWorkOrder(workOrder);
        schedule.setClient(quote.getClient());
        schedule.setApprovalDate(approval);
        schedule.setDeadlineDate(workOrder.getInstallDate());
        schedule.setStatus(Schedule.STATUS_AGUARDANDO_AGENDAMENTO);

        schedule = scheduleRepo.saveAndFlush(schedule);

        addHistory(schedule, "Criado", null, "Agenda gerada automaticamente na aprovação do orçamento " + quote.getNumber() + ".");

        return schedule;
    }


    @Transactional(readOnly = true)
    public List<ScheduleDto> listAllDto() {
        return scheduleRepo.findAllWithRelations().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduleDto findDto(UUID id) {
        Schedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + id));
        ScheduleDto dto = toDto(schedule);
        dto.setHistory(historyRepo.findByScheduleIdOrderByEventDateAsc(id));
        return dto;
    }


    @Transactional
    public String save(ScheduleSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("ID do agendamento é obrigatório.");
        }
        Schedule schedule = scheduleRepo.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + req.getId()));

        boolean isFirstScheduling = schedule.getScheduledDate() == null && req.getScheduledDate() != null;
        boolean isReschedule = !isFirstScheduling
                && req.getScheduledDate() != null
                && schedule.getScheduledDate() != null
                && !schedule.getScheduledDate().equals(req.getScheduledDate());

        LocalDate previousDate = schedule.getScheduledDate();

        if (req.getScheduledDate() != null) {
            schedule.setScheduledDate(req.getScheduledDate());
        }
        schedule.setScheduledTime(req.getScheduledTime());
        schedule.setResponsible(req.getResponsible());
        schedule.setTeam(req.getTeam());
        schedule.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        schedule.setObservations(req.getObservations());

        String newStatus = req.getStatus();
        if (newStatus == null || newStatus.isBlank()) {
            if (isReschedule) {
                newStatus = Schedule.STATUS_REAGENDADO;
            } else if (isFirstScheduling) {
                newStatus = Schedule.STATUS_AGENDADO;
            } else {
                newStatus = schedule.getStatus();
            }
        }
        schedule.setStatus(newStatus);

        String action;
        if (Schedule.STATUS_CANCELADO.equals(newStatus)) {
            action = "Cancelado";
        } else if (isReschedule) {
            action = "Reagendado";
            schedule.setRescheduleReason(req.getReason());
        } else if (isFirstScheduling) {
            action = "Agendado";
        } else if (Schedule.STATUS_CONFIRMADO.equals(newStatus)) {
            action = "Confirmado";
        } else if (Schedule.STATUS_EM_EXECUCAO.equals(newStatus)) {
            action = "Em Execução";
        } else if (Schedule.STATUS_CONCLUIDO.equals(newStatus)) {
            action = "Concluído";
        } else {
            action = "Editado";
        }

        String warning = null;
        if (schedule.getTeam() != null && !schedule.getTeam().isBlank() && schedule.getScheduledDate() != null) {
            boolean conflict = scheduleRepo.findAll().stream()
                    .filter(s -> !s.getId().equals(schedule.getId()))
                    .filter(s -> !Schedule.STATUS_CANCELADO.equals(s.getStatus()) && !Schedule.STATUS_CONCLUIDO.equals(s.getStatus()))
                    .anyMatch(s -> schedule.getScheduledDate().equals(s.getScheduledDate())
                            && schedule.getTeam().equalsIgnoreCase(s.getTeam() != null ? s.getTeam() : ""));
            if (conflict) {
                warning = "Atenção: a equipe \"" + schedule.getTeam() + "\" já possui outro serviço agendado para "
                        + schedule.getScheduledDate() + ".";
            }
        }

        scheduleRepo.saveAndFlush(schedule);

        String historyNotes = isReschedule && previousDate != null
                ? "De " + previousDate + " para " + schedule.getScheduledDate() + "."
                : req.getObservations();
        addHistory(schedule, action, req.getReason(), historyNotes);

        syncWorkOrder(schedule);

        return warning;
    }

    @Transactional
    public void cancel(UUID id, String reason) {
        Schedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + id));
        schedule.setStatus(Schedule.STATUS_CANCELADO);
        scheduleRepo.saveAndFlush(schedule);
        addHistory(schedule, "Cancelado", reason, null);
        syncWorkOrder(schedule);
    }


    @Transactional
    public void onQuoteCancelled(UUID quoteId) {
        scheduleRepo.findByQuoteId(quoteId).ifPresent(scheduleRepo::delete);
    }

    @Transactional
    public void onWorkOrderCancelled(UUID workOrderId) {
        scheduleRepo.findByWorkOrderId(workOrderId).ifPresent(schedule -> {
            schedule.setStatus(Schedule.STATUS_CANCELADO);
            scheduleRepo.saveAndFlush(schedule);
            addHistory(schedule, "Cancelado", "Ordem de Serviço vinculada foi cancelada.", null);
        });
    }

    @Transactional
    public void syncDeadlineFromWorkOrder(UUID workOrderId, LocalDate newDeadline) {
        if (newDeadline == null) return;
        scheduleRepo.findByWorkOrderId(workOrderId).ifPresent(schedule -> {
            if (!newDeadline.equals(schedule.getDeadlineDate())) {
                LocalDate oldDate = schedule.getDeadlineDate();
                schedule.setDeadlineDate(newDeadline);
                scheduleRepo.save(schedule);

                addHistory(schedule, "Editado", "Data de Instalação ajustada na Ordem de Serviço.",
                        "Data limite alterada de " + oldDate + " para " + newDeadline + ".");
            }
        });
    }


    @Transactional
    public int refreshOverdueStatuses() {
        LocalDate today = LocalDate.now();
        List<Schedule> candidates = scheduleRepo.findAll().stream()
                .filter(s -> !Schedule.STATUS_CONCLUIDO.equals(s.getStatus())
                        && !Schedule.STATUS_CANCELADO.equals(s.getStatus())
                        && !Schedule.STATUS_ATRASADO.equals(s.getStatus()))
                .filter(s -> s.getDeadlineDate() != null && s.getDeadlineDate().isBefore(today))
                .collect(Collectors.toList());

        for (Schedule s : candidates) {
            s.setStatus(Schedule.STATUS_ATRASADO);
            scheduleRepo.save(s);
            addHistory(s, "Atrasado", null, "Prazo limite (" + s.getDeadlineDate() + ") ultrapassado sem conclusão.");
        }
        return candidates.size();
    }


    @Transactional(readOnly = true)
    public Map<String, Object> getKpis() {
        List<Schedule> all = scheduleRepo.findAll();
        LocalDate today = LocalDate.now();
        LocalDate in7 = today.plusDays(7);

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("aprovados", (long) all.size());
        kpis.put("aguardandoAgendamento", all.stream().filter(s -> Schedule.STATUS_AGUARDANDO_AGENDAMENTO.equals(s.getStatus())).count());
        kpis.put("agendados", all.stream().filter(s -> Schedule.STATUS_AGENDADO.equals(s.getStatus())
                || Schedule.STATUS_CONFIRMADO.equals(s.getStatus())
                || Schedule.STATUS_REAGENDADO.equals(s.getStatus())).count());
        kpis.put("atrasados", all.stream().filter(s -> isLate(s, today)).count());
        kpis.put("semana", all.stream().filter(s -> s.getScheduledDate() != null
                && !s.getScheduledDate().isBefore(today) && !s.getScheduledDate().isAfter(in7)).count());

        List<Long> leadTimes = all.stream()
                .filter(s -> s.getScheduledDate() != null && s.getApprovalDate() != null)
                .map(s -> ChronoUnit.DAYS.between(s.getApprovalDate().toLocalDate(), s.getScheduledDate()))
                .collect(Collectors.toList());
        double avg = leadTimes.isEmpty() ? 0 : leadTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        kpis.put("prazoMedioDias", BigDecimal.valueOf(avg).setScale(1, java.math.RoundingMode.HALF_UP));

        return kpis;
    }


    private boolean isLate(Schedule s, LocalDate today) {
        if (Schedule.STATUS_CONCLUIDO.equals(s.getStatus()) || Schedule.STATUS_CANCELADO.equals(s.getStatus())) {
            return false;
        }
        return Schedule.STATUS_ATRASADO.equals(s.getStatus())
                || (s.getDeadlineDate() != null && s.getDeadlineDate().isBefore(today));
    }

    private void addHistory(Schedule schedule, String action, String reason, String notes) {
        ScheduleHistory h = new ScheduleHistory();
        h.setSchedule(schedule);
        h.setEventDate(LocalDateTime.now());
        h.setAction(action);
        h.setUsername(currentUsername());
        h.setReason(reason);
        h.setNotes(notes);
        historyRepo.save(h);
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
    }

    private void syncWorkOrder(Schedule schedule) {
        WorkOrder wo = schedule.getWorkOrder();
        if (wo == null) return;

        if (schedule.getScheduledDate() != null) {
            wo.setInstallDate(schedule.getScheduledDate());
        }

        if (!"cancelled".equals(wo.getStatus()) && !"completed".equals(wo.getStatus())) {
            switch (schedule.getStatus()) {
                case Schedule.STATUS_EM_EXECUCAO -> wo.setStatus("in_progress");
                case Schedule.STATUS_CONCLUIDO -> wo.setStatus("completed");
                case Schedule.STATUS_CANCELADO -> wo.setStatus("cancelled");
                default -> { }
            }
        }

        String existingNotes = wo.getNotes() != null ? wo.getNotes() : "";
        String withoutAgendaLine = AGENDA_NOTE_LINE.matcher(existingNotes).replaceAll("");

        StringBuilder agendaLine = new StringBuilder("[AGENDA] ");
        agendaLine.append("Execução: ").append(schedule.getScheduledDate() != null ? schedule.getScheduledDate() : "a definir");
        if (schedule.getScheduledTime() != null) {
            agendaLine.append(" ").append(schedule.getScheduledTime());
        }
        agendaLine.append(" | Status: ").append(statusLabel(schedule.getStatus()));
        if (schedule.getResponsible() != null && !schedule.getResponsible().isBlank()) {
            agendaLine.append(" | Responsável: ").append(schedule.getResponsible());
        }
        if (schedule.getTeam() != null && !schedule.getTeam().isBlank()) {
            agendaLine.append(" | Equipe: ").append(schedule.getTeam());
        }
        if (schedule.getObservations() != null && !schedule.getObservations().isBlank()) {
            agendaLine.append(" | Obs: ").append(schedule.getObservations());
        }

        String newNotes = (agendaLine + "\n" + withoutAgendaLine).trim();
        wo.setNotes(newNotes);

        workOrderRepo.save(wo);
    }

    private ScheduleDto toDto(Schedule s) {
        ScheduleDto dto = new ScheduleDto();
        dto.setId(s.getId());

        if (s.getQuote() != null) {
            dto.setQuoteId(s.getQuote().getId());
            dto.setQuoteNumber(s.getQuote().getNumber());
        }
        if (s.getWorkOrder() != null) {
            dto.setWorkOrderId(s.getWorkOrder().getId());
            dto.setWorkOrderNumber(s.getWorkOrder().getNumber());
            dto.setServiceType(deriveServiceType(s));
        }
        if (s.getClient() != null) {
            dto.setClientId(s.getClient().getId());
            dto.setClientName(s.getClient().getName());

            String addr = s.getClient().getAddress() != null ? s.getClient().getAddress() : "";
            String city = s.getClient().getCity() != null ? s.getClient().getCity() : "";
            if (!addr.isBlank() && !city.isBlank()) {
                dto.setClientAddress(addr + " - " + city);
            } else if (!addr.isBlank()) {
                dto.setClientAddress(addr);
            } else if (!city.isBlank()) {
                dto.setClientAddress(city);
            } else {
                dto.setClientAddress("Endereço não informado");
            }
        }

        dto.setApprovalDate(s.getApprovalDate());
        dto.setDeadlineDate(s.getDeadlineDate());
        dto.setScheduledDate(s.getScheduledDate());
        dto.setScheduledTime(s.getScheduledTime());
        dto.setServiceDetails(deriveServiceDetails(s));
        dto.setEstimatedDurationMinutes(s.getEstimatedDurationMinutes());
        dto.setStatus(s.getStatus());
        dto.setStatusLabel(statusLabel(s.getStatus()));
        dto.setResponsible(s.getResponsible());
        dto.setTeam(s.getTeam());
        dto.setObservations(s.getObservations());
        dto.setRescheduleReason(s.getRescheduleReason());

        LocalDate today = LocalDate.now();
        if (s.getDeadlineDate() != null) {
            dto.setDaysRemaining(ChronoUnit.DAYS.between(today, s.getDeadlineDate()));
        }
        dto.setSemaphore(computeSemaphore(s, today));

        return dto;
    }

    private List<String> deriveServiceDetails(Schedule s) {
        if (s.getQuote() != null && s.getQuote().getItems() != null && !s.getQuote().getItems().isEmpty()) {
            return s.getQuote().getItems().stream()
                    .map(item -> {
                        String cat = item.getCategory() != null ? item.getCategory() : "Item";
                        String prod = item.getProduct() != null ? item.getProduct() : "";
                        return cat + (prod.isBlank() ? "" : " (" + prod + ")");
                    })
                    .collect(Collectors.toList());
        }
        return List.of("Serviço sem itens detalhados");
    }

    private String deriveServiceType(Schedule s) {
        WorkOrder wo = s.getWorkOrder();
        if (wo.getCategory() != null && wo.getCategory().getName() != null) {
            return wo.getCategory().getName();
        }
        if (s.getQuote() != null && s.getQuote().getItems() != null && !s.getQuote().getItems().isEmpty()) {
            QuoteItem first = s.getQuote().getItems().get(0);
            if (first.getCategory() != null) return first.getCategory();
            if (first.getProduct() != null) return first.getProduct();
        }
        return "Serviço";
    }

    private String computeSemaphore(Schedule s, LocalDate today) {
        if (Schedule.STATUS_CONCLUIDO.equals(s.getStatus()) || Schedule.STATUS_CANCELADO.equals(s.getStatus())) {
            return "NEUTRAL";
        }
        if (s.getDeadlineDate() == null) {
            return "NEUTRAL";
        }
        long days = ChronoUnit.DAYS.between(today, s.getDeadlineDate());
        if (days < 0) return "RED";
        if (days <= 7) return "YELLOW";
        return "GREEN";
    }

    public static String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case Schedule.STATUS_AGUARDANDO_AGENDAMENTO -> "Aguardando Agendamento";
            case Schedule.STATUS_AGENDADO -> "Agendado";
            case Schedule.STATUS_CONFIRMADO -> "Confirmado";
            case Schedule.STATUS_EM_EXECUCAO -> "Em Execução";
            case Schedule.STATUS_CONCLUIDO -> "Concluído";
            case Schedule.STATUS_ATRASADO -> "Atrasado";
            case Schedule.STATUS_REAGENDADO -> "Reagendado";
            case Schedule.STATUS_CANCELADO -> "Cancelado";
            default -> status;
        };
    }
}
