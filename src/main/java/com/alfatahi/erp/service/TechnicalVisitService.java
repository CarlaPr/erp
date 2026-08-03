package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.TechnicalVisitDto;
import com.alfatahi.erp.dto.TechnicalVisitSaveRequest;
import com.alfatahi.erp.entity.Quote;
import com.alfatahi.erp.entity.TechnicalVisit;
import com.alfatahi.erp.repository.QuoteRepository;
import com.alfatahi.erp.repository.TechnicalVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TechnicalVisitService {

    private final TechnicalVisitRepository visitRepo;
    private final QuoteRepository quoteRepo;

    public TechnicalVisitService(TechnicalVisitRepository visitRepo, QuoteRepository quoteRepo) {
        this.visitRepo = visitRepo;
        this.quoteRepo = quoteRepo;
    }

    @Transactional
    public TechnicalVisitDto scheduleForQuote(UUID quoteId, TechnicalVisitSaveRequest req) {
        Quote quote = quoteRepo.findById(quoteId)
                .orElseThrow(() -> new IllegalArgumentException("Orçamento não encontrado: " + quoteId));

        if (quote.getClient() == null) {
            throw new IllegalStateException("Este orçamento não possui um cliente vinculado. Vincule um cliente antes de agendar a visita técnica.");
        }
        if (req.getVisitDate() == null) {
            throw new IllegalArgumentException("Informe a data da visita técnica.");
        }

        TechnicalVisit visit = new TechnicalVisit();
        visit.setQuote(quote);
        visit.setClient(quote.getClient());
        visit.setVisitDate(req.getVisitDate());
        visit.setVisitTime(req.getVisitTime());
        visit.setNotes(req.getNotes());

        visit = visitRepo.saveAndFlush(visit);
        return toDto(visit);
    }

    @Transactional(readOnly = true)
    public List<TechnicalVisitDto> listAllDto() {
        return visitRepo.findAllWithRelations().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TechnicalVisitDto findDto(UUID id) {
        TechnicalVisit visit = visitRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visita técnica não encontrada: " + id));
        return toDto(visit);
    }

    @Transactional
    public void update(TechnicalVisitSaveRequest req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("ID da visita técnica é obrigatório.");
        }
        if (req.getVisitDate() == null) {
            throw new IllegalArgumentException("Informe a data da visita técnica.");
        }

        TechnicalVisit visit = visitRepo.findById(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Visita técnica não encontrada: " + req.getId()));

        visit.setVisitDate(req.getVisitDate());
        visit.setVisitTime(req.getVisitTime());
        visit.setNotes(req.getNotes());

        visitRepo.saveAndFlush(visit);
    }

    @Transactional
    public void delete(UUID id) {
        if (!visitRepo.existsById(id)) {
            throw new IllegalArgumentException("Visita técnica não encontrada: " + id);
        }
        visitRepo.deleteById(id);
    }

    @Transactional
    public void onQuoteDeleted(UUID quoteId) {
        List<TechnicalVisit> visits = visitRepo.findByQuoteId(quoteId);
        if (visits != null && !visits.isEmpty()) {
            visitRepo.deleteAll(visits);
        }
    }

    private TechnicalVisitDto toDto(TechnicalVisit v) {
        TechnicalVisitDto dto = new TechnicalVisitDto();
        dto.setId(v.getId());

        if (v.getQuote() != null) {
            dto.setQuoteId(v.getQuote().getId());
            dto.setQuoteNumber(v.getQuote().getNumber());
        }

        if (v.getClient() != null) {
            dto.setClientId(v.getClient().getId());
            dto.setClientName(v.getClient().getName());

            String addr = v.getClient().getAddress() != null ? v.getClient().getAddress() : "";
            String city = v.getClient().getCity() != null ? v.getClient().getCity() : "";
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

        dto.setVisitDate(v.getVisitDate());
        dto.setVisitTime(v.getVisitTime());
        dto.setNotes(v.getNotes());
        dto.setCreatedAt(v.getCreatedAt());

        return dto;
    }
}
