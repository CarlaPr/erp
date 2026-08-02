package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.ExpenseRecurrenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
public class RecurrenceService {


    private static final int BATCH_SIZE = 60;


    private static final int INFINITE_HORIZON_MONTHS = 6;

    private final ExpenseRecurrenceRepository recurrenceRepository;
    private final AccountsPayableRepository payableRepository;

    public RecurrenceService(ExpenseRecurrenceRepository recurrenceRepository,
                              AccountsPayableRepository payableRepository) {
        this.recurrenceRepository = recurrenceRepository;
        this.payableRepository = payableRepository;
    }



    @Transactional
    public List<AccountsPayable> createRecurrence(AccountsPayable template,
                                                   RecurrenceFrequency frequency,
                                                   RecurrenceEndType endType,
                                                   Integer occurrenceCount,
                                                   LocalDate endDate) {

        ExpenseRecurrence rec = new ExpenseRecurrence();
        rec.setDescription(template.getDescription());
        rec.setCategory(template.getCategory());
        rec.setSubcategory(template.getSubcategory());
        rec.setExpenseType(template.getExpenseType());
        rec.setExpenseNature(template.getExpenseNature());
        rec.setCostCenter(template.getCostCenter());
        rec.setBaseAmount(template.getTotalAmount());
        rec.setSupplier(template.getSupplier());
        rec.setWorkOrder(template.getWorkOrder());
        rec.setPaymentMethod(template.getPaymentMethod());
        rec.setDocumentNumber(template.getDocumentNumber());
        rec.setNotes(template.getNotes());
        rec.setFrequency(frequency);
        rec.setStartDate(template.getDueDate());
        rec.setEndType(endType);
        rec.setOccurrenceCount(endType == RecurrenceEndType.COUNT ? occurrenceCount : null);
        rec.setEndDate(endType == RecurrenceEndType.DATE ? endDate : null);
        rec.setStatus(RecurrenceStatus.ACTIVE);
        rec.setTotalGenerated(0);
        rec = recurrenceRepository.save(rec);

        List<AccountsPayable> created = generateNextBatch(rec, BATCH_SIZE);
        recurrenceRepository.save(rec);
        return created;
    }


    private List<AccountsPayable> generateNextBatch(ExpenseRecurrence rec, int maxToGenerate) {
        List<AccountsPayable> created = new ArrayList<>();
        if (rec.getStatus() != RecurrenceStatus.ACTIVE) return created;

        LocalDate cursor = rec.getLastGeneratedDueDate();
        int seq = rec.getTotalGenerated();

        for (int i = 0; i < maxToGenerate; i++) {
            LocalDate dueDate = (cursor == null) ? rec.getStartDate() : rec.getFrequency().next(cursor);

            if (rec.getEndType() == RecurrenceEndType.COUNT
                    && rec.getOccurrenceCount() != null
                    && seq + 1 > rec.getOccurrenceCount()) {
                rec.setStatus(RecurrenceStatus.COMPLETED);
                break;
            }
            if (rec.getEndType() == RecurrenceEndType.DATE
                    && rec.getEndDate() != null
                    && dueDate.isAfter(rec.getEndDate())) {
                rec.setStatus(RecurrenceStatus.COMPLETED);
                break;
            }

            seq++;
            AccountsPayable ap = buildInstallment(rec, dueDate, seq);
            created.add(payableRepository.save(ap));
            cursor = dueDate;
        }

        rec.setLastGeneratedDueDate(cursor);
        rec.setTotalGenerated(seq);
        return created;
    }

    private AccountsPayable buildInstallment(ExpenseRecurrence rec, LocalDate dueDate, int seq) {
        AccountsPayable ap = new AccountsPayable();
        ap.setDescription(rec.getDescription());
        ap.setCategory(rec.getCategory());
        ap.setSubcategory(rec.getSubcategory());
        ap.setExpenseType(rec.getExpenseType());
        ap.setExpenseNature(rec.getExpenseNature());
        ap.setCostCenter(rec.getCostCenter());
        ap.setTotalAmount(rec.getBaseAmount());
        ap.setPaidAmount(BigDecimal.ZERO);
        ap.setDueDate(dueDate);
        ap.setCompetencia(dueDate.withDayOfMonth(1));
        ap.setStatus("pending");
        ap.setPaymentMethod(rec.getPaymentMethod());
        ap.setDocumentNumber(rec.getDocumentNumber());
        ap.setNotes(rec.getNotes());
        ap.setSupplier(rec.getSupplier());
        ap.setWorkOrder(rec.getWorkOrder());
        ap.setRecurring(true);
        ap.setRecurrenceId(rec.getId());
        ap.setRecurrenceSeq(seq);
        return ap;
    }



    @Transactional
    public int topUpAllActiveRecurrences() {
        int totalCreated = 0;
        LocalDate horizonLimit = LocalDate.now().plusMonths(INFINITE_HORIZON_MONTHS);

        for (ExpenseRecurrence rec : recurrenceRepository.findByStatus(RecurrenceStatus.ACTIVE)) {
            boolean needsMore;
            if (rec.getEndType() == RecurrenceEndType.INFINITE) {
                needsMore = rec.getLastGeneratedDueDate() == null || rec.getLastGeneratedDueDate().isBefore(horizonLimit);
            } else {
                needsMore = true;
            }
            if (!needsMore) continue;

            List<AccountsPayable> created = generateNextBatch(rec, BATCH_SIZE);
            totalCreated += created.size();
            recurrenceRepository.save(rec);
        }
        return totalCreated;
    }



    @Transactional
    public void deleteSingleInstallment(UUID payableId) {
        AccountsPayable ap = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        removeOrCancel(ap);
    }


    @Transactional
    public int deleteCurrentAndFuture(UUID payableId) {
        AccountsPayable current = payableRepository.findById(payableId)
                .orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        if (current.getRecurrenceId() == null) {
            removeOrCancel(current);
            return 1;
        }
        List<AccountsPayable> toRemove = payableRepository
                .findByRecurrenceIdAndDueDateGreaterThanEqualOrderByDueDateAsc(current.getRecurrenceId(), current.getDueDate());
        toRemove.forEach(this::removeOrCancel);

        recurrenceRepository.findById(current.getRecurrenceId()).ifPresent(rec -> {
            rec.setStatus(RecurrenceStatus.CANCELLED);
            recurrenceRepository.save(rec);
        });
        return toRemove.size();
    }


    @Transactional
    public int deleteEntireSeries(UUID recurrenceId) {
        List<AccountsPayable> all = payableRepository.findByRecurrenceIdOrderByDueDateAsc(recurrenceId);
        all.forEach(this::removeOrCancel);

        recurrenceRepository.findById(recurrenceId).ifPresent(rec -> {
            rec.setStatus(RecurrenceStatus.CANCELLED);
            recurrenceRepository.save(rec);
        });
        return all.size();
    }

    private void removeOrCancel(AccountsPayable ap) {
        if ("pending".equals(ap.getStatus())) {
            payableRepository.delete(ap);
        } else if (!"cancelled".equals(ap.getStatus())) {
            ap.setStatus("cancelled");
            payableRepository.save(ap);
        }
    }



    @Transactional
    public int editEntireSeries(UUID recurrenceId, AccountsPayable changes) {
        ExpenseRecurrence rec = recurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new RuntimeException("Recorrência não encontrada"));

        rec.setDescription(changes.getDescription());
        rec.setCategory(changes.getCategory());
        rec.setSubcategory(changes.getSubcategory());
        rec.setExpenseType(changes.getExpenseType());
        rec.setExpenseNature(changes.getExpenseNature());
        rec.setCostCenter(changes.getCostCenter());
        rec.setBaseAmount(changes.getTotalAmount());
        rec.setPaymentMethod(changes.getPaymentMethod());
        rec.setNotes(changes.getNotes());
        recurrenceRepository.save(rec);

        List<AccountsPayable> installments = payableRepository.findByRecurrenceIdOrderByDueDateAsc(recurrenceId);
        int updated = 0;
        for (AccountsPayable ap : installments) {
            if ("paid".equals(ap.getStatus()) || "cancelled".equals(ap.getStatus())) continue;
            ap.setDescription(changes.getDescription());
            ap.setCategory(changes.getCategory());
            ap.setSubcategory(changes.getSubcategory());
            ap.setExpenseType(changes.getExpenseType());
            ap.setExpenseNature(changes.getExpenseNature());
            ap.setCostCenter(changes.getCostCenter());
            if (changes.getTotalAmount() != null) ap.setTotalAmount(changes.getTotalAmount());
            if (changes.getPaymentMethod() != null) ap.setPaymentMethod(changes.getPaymentMethod());
            if (changes.getNotes() != null) ap.setNotes(changes.getNotes());
            payableRepository.save(ap);
            updated++;
        }
        return updated;
    }


    @Transactional
    public void cancelFutureOccurrences(UUID recurrenceId) {
        ExpenseRecurrence rec = recurrenceRepository.findById(recurrenceId)
                .orElseThrow(() -> new RuntimeException("Recorrência não encontrada"));
        rec.setStatus(RecurrenceStatus.CANCELLED);
        recurrenceRepository.save(rec);
    }
}
