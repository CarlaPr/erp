package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.AgendaDayBlockDto;
import com.alfatahi.erp.entity.AgendaDayBlock;
import com.alfatahi.erp.exception.AgendaDayBlockedException;
import com.alfatahi.erp.repository.AgendaDayBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AgendaDayBlockService {
    private final AgendaDayBlockRepository repository;

    public AgendaDayBlockService(AgendaDayBlockRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AgendaDayBlockDto> findAll() {
        return repository.findAllByOrderByDateAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(LocalDate date) {
        return date != null && repository.existsById(date);
    }

    @Transactional(readOnly = true)
    public void assertNotBlocked(LocalDate date) {
        if (date == null) return;
        repository.findById(date).ifPresent(block -> {
            throw new AgendaDayBlockedException(date, block.getReason());
        });
    }

    @Transactional
    public AgendaDayBlockDto block(LocalDate date, String reason) {
        if (date == null) throw new IllegalArgumentException("Informe a data que deseja bloquear.");
        AgendaDayBlock block = repository.findById(date).orElseGet(AgendaDayBlock::new);
        block.setDate(date);
        block.setReason(cleanReason(reason));
        return toDto(repository.save(block));
    }

    @Transactional
    public void unblock(LocalDate date) {
        if (date != null) repository.deleteById(date);
    }

    private String cleanReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        String trimmed = reason.trim();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private AgendaDayBlockDto toDto(AgendaDayBlock block) {
        return new AgendaDayBlockDto(block.getDate(), block.getReason());
    }
}
