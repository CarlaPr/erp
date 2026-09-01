package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.PendingScheduleAlertDto;
import com.alfatahi.erp.dto.ScheduleDto;
import com.alfatahi.erp.dto.ScheduleOccurrenceDto;
import com.alfatahi.erp.dto.ScheduleOccurrenceSaveRequest;
import com.alfatahi.erp.dto.ScheduleSaveRequest;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.ScheduleHistoryRepository;
import com.alfatahi.erp.repository.ScheduleOccurrenceRepository;
import com.alfatahi.erp.repository.ScheduleRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.util.DeliveryDeadline;
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
    private final ScheduleOccurrenceRepository occurrenceRepo;
    private final WorkOrderRepository workOrderRepo;

    public ScheduleService(ScheduleRepository scheduleRepo, ScheduleHistoryRepository historyRepo,
                           ScheduleOccurrenceRepository occurrenceRepo, WorkOrderRepository workOrderRepo) {
        this.scheduleRepo = scheduleRepo;
        this.historyRepo = historyRepo;
        this.occurrenceRepo = occurrenceRepo;
        this.workOrderRepo = workOrderRepo;
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> findWorkOrderByQuoteId(UUID quoteId) {
        return scheduleRepo.findByQuoteId(quoteId).map(Schedule::getWorkOrder);
    }

    @Transactional
    public Schedule createFromApprovedQuote(Quote quote, WorkOrder workOrder) {
        Optional<Schedule> existing = scheduleRepo.findByQuoteId(quote.getId());
        if (existing.isPresent()) {
            if (!Objects.equals(existing.get().getWorkOrder().getId(), workOrder.getId())) {
                throw new IllegalStateException("O orçamento já possui uma agenda vinculada a outra O.S.");
            }
            return existing.get();
        }

        LocalDateTime approval = quote.getDateApproved() != null ? quote.getDateApproved() : LocalDateTime.now();

        Schedule schedule = new Schedule();
        schedule.setQuote(quote);
        schedule.setWorkOrder(workOrder);
        schedule.setClient(quote.getClient());
        schedule.setApprovalDate(approval);
        schedule.setDeadlineDate(DeliveryDeadline.fromApproval(approval));
        schedule.setStatus(Schedule.STATUS_AGUARDANDO_AGENDAMENTO);

        workOrder.setDeadlineDate(schedule.getDeadlineDate());
        workOrder.setInstallDate(null);
        workOrderRepo.save(workOrder);

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

    /**
     * Ordens de serviço com prazo de entrega expirando em até {@code daysThreshold} dias (ou já vencido)
     * que ainda não possuem nenhum agendamento (data marcada). Usado para o alerta de atenção da role VENDAS.
     */
    @Transactional(readOnly = true)
    public List<PendingScheduleAlertDto> listPendingScheduleAlerts(int daysThreshold) {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(daysThreshold);

        return scheduleRepo.findAllWithRelations().stream()
                .filter(s -> s.getScheduledDate() == null)
                .filter(s -> !Schedule.STATUS_CANCELADO.equals(s.getStatus()) && !Schedule.STATUS_CONCLUIDO.equals(s.getStatus()))
                .filter(s -> s.getWorkOrder() != null
                        && !"cancelled".equals(s.getWorkOrder().getStatus())
                        && !"completed".equals(s.getWorkOrder().getStatus()))
                .filter(s -> s.getDeadlineDate() != null && !s.getDeadlineDate().isAfter(limit))
                .sorted(Comparator.comparing(Schedule::getDeadlineDate))
                .map(s -> toPendingAlertDto(s, today))
                .collect(Collectors.toList());
    }

    private PendingScheduleAlertDto toPendingAlertDto(Schedule s, LocalDate today) {
        PendingScheduleAlertDto dto = new PendingScheduleAlertDto();
        dto.setScheduleId(s.getId());

        if (s.getWorkOrder() != null) {
            dto.setWorkOrderId(s.getWorkOrder().getId());
            dto.setWorkOrderNumber(s.getWorkOrder().getNumber());
            dto.setServiceType(deriveServiceType(s));
        }
        if (s.getQuote() != null) {
            dto.setQuoteId(s.getQuote().getId());
            dto.setQuoteNumber(s.getQuote().getNumber());
        }
        if (s.getClient() != null) {
            dto.setClientId(s.getClient().getId());
            dto.setClientName(s.getClient().getName());
        }

        dto.setServiceDetails(deriveServiceDetails(s));
        dto.setDeadlineDate(s.getDeadlineDate());

        long days = ChronoUnit.DAYS.between(today, s.getDeadlineDate());
        dto.setDaysRemaining(days);
        dto.setOverdue(days < 0);

        return dto;
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
    public void addTechnicianNote(UUID scheduleId, String notes) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Escreva uma observação antes de salvar.");
        }
        Schedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + scheduleId));

        addHistory(schedule, "Observação do Técnico", null, notes.trim());
    }

    public List<Schedule> findByDate(LocalDate date) {
        return scheduleRepo.findAll().stream()
                .filter(s -> s.getScheduledDate() != null
                        && s.getScheduledDate().equals(date))
                .collect(Collectors.toList());
    }

    @Transactional
    public String save(ScheduleSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("ID do agendamento é obrigatório.");
        }

        final Schedule schedule = scheduleRepo.findById(req.getId())
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

        if (req.getScheduledDate() != null) {
            ScheduleOccurrence occ = (previousDate != null
                    ? occurrenceRepo.findByScheduleIdAndOccurrenceDate(schedule.getId(), previousDate)
                    : Optional.<ScheduleOccurrence>empty())
                    .orElseGet(() -> {
                        ScheduleOccurrence created = new ScheduleOccurrence();
                        created.setSchedule(schedule);
                        schedule.getOccurrences().add(created);
                        return created;
                    });
            occ.setOccurrenceDate(req.getScheduledDate());
            occ.setOccurrenceTime(req.getScheduledTime());
            occ.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
            occ.setResponsible(req.getResponsible());
            occ.setTeam(req.getTeam());
            occ.setObservations(req.getObservations());
            occurrenceRepo.save(occ);
        }

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

        final String statusForLambda = newStatus;

        if (req.getScheduledDate() != null) {
            occurrenceRepo.findByScheduleIdAndOccurrenceDate(schedule.getId(), req.getScheduledDate())
                    .ifPresent(occ -> occ.setStatus(statusForLambda));
        }

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

        Schedule scheduleToSync = schedule;

        if (req.getExtraOccurrences() != null) {
            for (ScheduleOccurrenceSaveRequest extra : req.getExtraOccurrences()) {
                if (extra.getOccurrenceDate() == null) {
                    continue;
                }
                extra.setScheduleId(schedule.getId());
                if (extra.getOccurrenceId() != null) {
                    updateOccurrence(extra);
                } else {
                    boolean alreadyExists = occurrenceRepo
                            .findByScheduleIdAndOccurrenceDate(schedule.getId(), extra.getOccurrenceDate())
                            .isPresent();
                    if (!alreadyExists) {
                        addOccurrence(extra);
                    }
                }
            }
            scheduleToSync = scheduleRepo.findById(schedule.getId()).orElse(schedule);
        }

        resyncHeaderFromOccurrences(scheduleToSync);
        scheduleRepo.saveAndFlush(scheduleToSync);
        syncWorkOrder(scheduleToSync);

        return warning;
    }

    @Transactional
    public void cancel(UUID id, String reason) {
        Schedule schedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + id));
        schedule.setStatus(Schedule.STATUS_CANCELADO);
        schedule.getOccurrences().forEach(o -> o.setStatus(Schedule.STATUS_CANCELADO));
        scheduleRepo.saveAndFlush(schedule);
        addHistory(schedule, "Cancelado", reason, null);
        syncWorkOrder(schedule);
    }

    @Transactional
    public void changeDeadline(UUID scheduleId, LocalDate newDeadline, String reason) {
        if (newDeadline == null) {
            throw new IllegalArgumentException("Informe a nova data de prazo.");
        }
        Schedule schedule = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + scheduleId));

        LocalDate oldDeadline = schedule.getDeadlineDate();
        if (Objects.equals(oldDeadline, newDeadline)) {
            return;
        }
        schedule.setDeadlineDate(newDeadline);

        if (Schedule.STATUS_ATRASADO.equals(schedule.getStatus()) && !newDeadline.isBefore(LocalDate.now())) {
            schedule.setStatus(schedule.getScheduledDate() != null ? Schedule.STATUS_AGENDADO : Schedule.STATUS_AGUARDANDO_AGENDAMENTO);
        }

        scheduleRepo.saveAndFlush(schedule);
        addHistory(schedule, "Prazo Alterado", reason,
                "Prazo alterado de " + fmt(oldDeadline) + " para " + fmt(newDeadline) + ".");

        WorkOrder wo = schedule.getWorkOrder();
        if (wo != null) {
            wo.setDeadlineDate(newDeadline);
            workOrderRepo.save(wo);
        }
    }

    @Transactional
    public ScheduleOccurrenceDto addOccurrence(ScheduleOccurrenceSaveRequest req) {
        if (req.getScheduleId() == null) {
            throw new IllegalArgumentException("Agendamento (scheduleId) é obrigatório.");
        }
        if (req.getOccurrenceDate() == null) {
            throw new IllegalArgumentException("Informe a data do dia a agendar.");
        }
        Schedule schedule = scheduleRepo.findById(req.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado: " + req.getScheduleId()));

        occurrenceRepo.findByScheduleIdAndOccurrenceDate(schedule.getId(), req.getOccurrenceDate())
                .ifPresent(o -> {
                    throw new IllegalArgumentException("Este serviço já está agendado para " + fmt(req.getOccurrenceDate()) + ".");
                });

        ScheduleOccurrence occ = new ScheduleOccurrence();
        occ.setSchedule(schedule);
        occ.setOccurrenceDate(req.getOccurrenceDate());
        occ.setOccurrenceTime(req.getOccurrenceTime());
        occ.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        occ.setResponsible(req.getResponsible() != null ? req.getResponsible() : schedule.getResponsible());
        occ.setTeam(req.getTeam() != null ? req.getTeam() : schedule.getTeam());
        occ.setObservations(req.getObservations());
        occ.setStatus(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus() : Schedule.STATUS_AGENDADO);

        schedule.getOccurrences().add(occ);
        occ = occurrenceRepo.saveAndFlush(occ);

        resyncHeaderFromOccurrences(schedule);
        scheduleRepo.saveAndFlush(schedule);

        addHistory(schedule, "Dia Adicionado", req.getReason(),
                "Novo dia de execução agendado para " + fmt(occ.getOccurrenceDate())
                        + (occ.getOccurrenceTime() != null ? " às " + occ.getOccurrenceTime() : "") + ".");

        syncWorkOrder(schedule);

        return toOccurrenceDto(occ);
    }

    @Transactional
    public ScheduleOccurrenceDto updateOccurrence(ScheduleOccurrenceSaveRequest req) {
        if (req.getOccurrenceId() == null) {
            throw new IllegalArgumentException("ID da ocorrência é obrigatório.");
        }
        ScheduleOccurrence occ = occurrenceRepo.findById(req.getOccurrenceId())
                .orElseThrow(() -> new IllegalArgumentException("Data de agendamento não encontrada: " + req.getOccurrenceId()));
        Schedule schedule = occ.getSchedule();

        LocalDate oldDate = occ.getOccurrenceDate();

        if (req.getOccurrenceDate() != null && !req.getOccurrenceDate().equals(oldDate)) {
            occurrenceRepo.findByScheduleIdAndOccurrenceDate(schedule.getId(), req.getOccurrenceDate())
                    .filter(other -> !other.getId().equals(occ.getId()))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("Este serviço já está agendado para " + fmt(req.getOccurrenceDate()) + ".");
                    });
            occ.setOccurrenceDate(req.getOccurrenceDate());
        }
        if (req.getOccurrenceTime() != null) occ.setOccurrenceTime(req.getOccurrenceTime());
        if (req.getEstimatedDurationMinutes() != null) occ.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        if (req.getResponsible() != null) occ.setResponsible(req.getResponsible());
        if (req.getTeam() != null) occ.setTeam(req.getTeam());
        if (req.getObservations() != null) occ.setObservations(req.getObservations());
        if (req.getStatus() != null && !req.getStatus().isBlank()) occ.setStatus(req.getStatus());

        occurrenceRepo.saveAndFlush(occ);

        resyncHeaderFromOccurrences(schedule);
        scheduleRepo.saveAndFlush(schedule);

        String notes = !occ.getOccurrenceDate().equals(oldDate)
                ? "Dia reagendado de " + fmt(oldDate) + " para " + fmt(occ.getOccurrenceDate()) + "."
                : "Dia " + fmt(occ.getOccurrenceDate()) + " atualizado.";
        addHistory(schedule, "Dia Alterado", req.getReason(), notes);

        syncWorkOrder(schedule);

        return toOccurrenceDto(occ);
    }

    @Transactional
    public void removeOccurrence(UUID occurrenceId, String reason) {
        ScheduleOccurrence occ = occurrenceRepo.findById(occurrenceId)
                .orElseThrow(() -> new IllegalArgumentException("Data de agendamento não encontrada: " + occurrenceId));
        Schedule schedule = occ.getSchedule();
        LocalDate removedDate = occ.getOccurrenceDate();

        schedule.getOccurrences().removeIf(o -> o.getId().equals(occ.getId()));
        occurrenceRepo.delete(occ);

        resyncHeaderFromOccurrences(schedule);
        scheduleRepo.saveAndFlush(schedule);

        addHistory(schedule, "Dia Removido", reason, "Dia " + fmt(removedDate) + " removido da agenda.");

        syncWorkOrder(schedule);
    }

    private void resyncHeaderFromOccurrences(Schedule schedule) {
        List<ScheduleOccurrence> sorted = schedule.getOccurrencesSorted();

        if (sorted.isEmpty()) {
            schedule.setScheduledDate(null);
            schedule.setScheduledTime(null);
            if (!Schedule.STATUS_CANCELADO.equals(schedule.getStatus())) {
                schedule.setStatus(Schedule.STATUS_AGUARDANDO_AGENDAMENTO);
            }
            return;
        }

        LocalDate today = LocalDate.now();
        ScheduleOccurrence representative = sorted.stream()
                .filter(o -> !o.getOccurrenceDate().isBefore(today))
                .findFirst()
                .orElse(sorted.get(sorted.size() - 1));

        schedule.setScheduledDate(representative.getOccurrenceDate());
        schedule.setScheduledTime(representative.getOccurrenceTime());
        schedule.setEstimatedDurationMinutes(representative.getEstimatedDurationMinutes());
        if (representative.getResponsible() != null) schedule.setResponsible(representative.getResponsible());
        if (representative.getTeam() != null) schedule.setTeam(representative.getTeam());

        if (!Schedule.STATUS_CANCELADO.equals(schedule.getStatus())
                && !Schedule.STATUS_CONCLUIDO.equals(schedule.getStatus())) {
            schedule.setStatus(representative.getStatus());
        }
    }

    private ScheduleOccurrenceDto toOccurrenceDto(ScheduleOccurrence o) {
        ScheduleOccurrenceDto dto = new ScheduleOccurrenceDto();
        dto.setId(o.getId());
        dto.setOccurrenceDate(o.getOccurrenceDate());
        dto.setOccurrenceTime(o.getOccurrenceTime());
        dto.setEstimatedDurationMinutes(o.getEstimatedDurationMinutes());
        dto.setStatus(o.getStatus());
        dto.setStatusLabel(statusLabel(o.getStatus()));
        dto.setResponsible(o.getResponsible());
        dto.setTeam(o.getTeam());
        dto.setObservations(o.getObservations());
        return dto;
    }

    private String fmt(LocalDate date) {
        return date == null ? "-" : date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Transactional
    public void onQuoteCancelled(UUID quoteId) {
        scheduleRepo.findByQuoteId(quoteId).ifPresent(this::deleteScheduleAndHistory);
    }

    @Transactional
    public void onWorkOrderCancelled(UUID workOrderId) {
        scheduleRepo.findByWorkOrderId(workOrderId).ifPresent(schedule -> {
            if (Schedule.STATUS_CANCELADO.equals(schedule.getStatus())) return;
            schedule.setStatus(Schedule.STATUS_CANCELADO);
            scheduleRepo.saveAndFlush(schedule);
            addHistory(schedule, "Cancelado", "Ordem de Serviço vinculada foi cancelada.", null);
        });
    }

    @Transactional
    public void onWorkOrderDeleted(UUID workOrderId) {
        scheduleRepo.findByWorkOrderId(workOrderId).ifPresent(this::deleteScheduleAndHistory);
    }

    private void deleteScheduleAndHistory(Schedule schedule) {
        List<ScheduleHistory> historyList = historyRepo.findByScheduleIdOrderByEventDateAsc(schedule.getId());
        if (historyList != null && !historyList.isEmpty()) {
            historyRepo.deleteAll(historyList);
        }
        occurrenceRepo.deleteByScheduleId(schedule.getId());
        scheduleRepo.delete(schedule);
    }

    @Transactional
    public void applyAgendaDates(WorkOrder workOrder) {
        if (workOrder.getId() == null) return;
        scheduleRepo.findByWorkOrderId(workOrder.getId()).ifPresent(schedule -> {
            workOrder.setDeadlineDate(schedule.getDeadlineDate());
            workOrder.setInstallDate(schedule.getScheduledDate());
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

        wo.setDeadlineDate(schedule.getDeadlineDate());
        wo.setInstallDate(schedule.getScheduledDate());

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

        dto.setOccurrences(s.getOccurrencesSorted().stream()
                .map(this::toOccurrenceDto)
                .collect(Collectors.toList()));

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