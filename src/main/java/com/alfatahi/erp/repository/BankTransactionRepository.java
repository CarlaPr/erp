package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {
    List<BankTransaction> findAllByOrderByTransactionDateDesc();
    boolean existsByExternalId(String externalId);
}