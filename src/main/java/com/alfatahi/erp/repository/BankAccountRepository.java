package com.alfatahi.erp.repository;

import com.alfatahi.erp.dto.BankAccountBalance;
import com.alfatahi.erp.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findAllByOpeningBalanceIsNotNullOrderByNameAsc();

    Optional<BankAccount> findByIdAndOpeningBalanceIsNotNull(UUID id);

    @Modifying
    @Query("UPDATE BankAccount b SET b.openingBalance = :openingBalance WHERE b.id = :id AND b.openingBalance IS NOT NULL")
    int updateOpeningBalance(UUID id, BigDecimal openingBalance);

    @Modifying
    @Query("DELETE FROM BankAccount b WHERE b.id = :id AND b.openingBalance IS NOT NULL")
    int deleteConfiguredAccount(UUID id);

    @Query("""
            SELECT new com.alfatahi.erp.dto.BankAccountBalance(b.id, b.name, b.openingBalance,
                b.openingBalance + SUM(CASE
                    WHEN r.status IN ('received', 'partial') THEN m.amount
                    WHEN p.status IN ('paid', 'partial') THEN -m.amount
                    ELSE 0
                END))
            FROM BankAccount b
            LEFT JOIN FinancialMovement m ON m.bankAccount = b AND m.movementDate <= :asOf
            LEFT JOIN m.accountsReceivable r
            LEFT JOIN m.accountsPayable p
            WHERE b.openingBalance IS NOT NULL
            GROUP BY b.id, b.name, b.openingBalance
            ORDER BY b.name
            """)
    List<BankAccountBalance> findBalances(LocalDate asOf);
}
